import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Application {
    private static final int HTTP_PORT = 8080;
    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;

    public static void main(String[] args) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);

        httpServer.createContext("/", httpExchange -> {
            String respText = "Hello! Привет! (root) \n";
            httpExchange.sendResponseHeaders(200, respText.getBytes().length);
            OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(respText.getBytes());
            outputStream.flush();
            httpExchange.close();
        });

        httpServer.createContext("/visited_links", httpExchange -> {
            if ("POST".equals(httpExchange.getRequestMethod())) {
                InputStream inputStream = httpExchange.getRequestBody();

                String status = handlePOST(inputStream) + "\n";
                inputStream.close();

                httpExchange.sendResponseHeaders(200, status.getBytes().length);
                OutputStream output = httpExchange.getResponseBody();
                output.write(status.getBytes());
                output.flush();
            } else {
                httpExchange.sendResponseHeaders(405, -1);  // 405 Method Not Allowed
            }
            httpExchange.close();
        });

        httpServer.createContext("/visited_domains", httpExchange -> {
            if ("GET".equals(httpExchange.getRequestMethod())) {
                String respText = handleGET(httpExchange.getRequestURI().getRawQuery());
                httpExchange.sendResponseHeaders(200, respText.getBytes().length);
                OutputStream output = httpExchange.getResponseBody();
                output.write(respText.getBytes());
                output.flush();
            } else {
                httpExchange.sendResponseHeaders(405, -1);  // 405 Method Not Allowed
            }
            httpExchange.close();
        });

        httpServer.setExecutor(null);   // default executor
        httpServer.start();


    }


    public static String handleGET(String query) throws IOException {
        Set<String> domains = new HashSet<>();
        String status = checkGetQuery(query);

        if (status.startsWith("Ok")) {
            String[] statusArr = status.split(" ");
            status = "Ok";
            double dateFrom = Double.parseDouble(statusArr[1]);   // здесь можно не проверять на
            double dateTo = Double.parseDouble(statusArr[2]);     // исключение т.к. уже проверено в
            // методе checkGetQuery

            // получаем данные из Redis и заполняем domains
            // описание структуры данных в Redis см. в методе handlePOST
            try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
                jedis.connect();
                Set<String> keys = jedis.zrangeByScore("funbox", dateFrom, dateTo);
                for (String key : keys) {
                    List<String> links = jedis.lrange(key, 0, -1);
                    for (String link : links) {
                        // из линка выделяем домен
                        if (link.startsWith("http://")) link = link.substring(7);
                        if (link.startsWith("https://")) link = link.substring(8);
                        link = link.split("\\/|\\?")[0];
                        String[] temp = link.split("\\.");
                        if (temp.length > 2) {
                            link = temp[temp.length - 2] + "." + temp[temp.length - 1];
                        }
                        // в domains добавляем "очищенный" линк
                        domains.add(link);
                    }
                }

            } catch (Exception e) {
                status = "Failed. Can't read links: " + e;
            }

        }


        ObjectForGET objectForGET = new ObjectForGET(domains, status);
        StringWriter writer = new StringWriter();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(writer, objectForGET);

        return writer.toString();
    }

    public static String checkGetQuery(String query) {
        if (query == null || query.isEmpty()) {
            return "Empty Query";
        }

        String[] queryArr = query.split("=|&");
        if (queryArr.length != 4) {
            return "Wrong Query";
        }

        double dateFrom = 0, dateTo = 0;
        try {
            dateFrom = Double.parseDouble(queryArr[1]);
            dateTo = Double.parseDouble(queryArr[3]);
        } catch (NumberFormatException e) {
            return "Wrong Dates";
        }

        if (dateTo < dateFrom) {
            return "TO date is earlier than FROM date";
        }

        return "Ok " + queryArr[1] + " " + queryArr[3];
    }

    public static String handlePOST(InputStream inputStream) throws IOException {
        ObjectStatusOnly statusOnly = new ObjectStatusOnly();
        ObjectMapper mapper = new ObjectMapper();
        try {
            ObjectForPOST objectForPOST = mapper.readValue(inputStream, ObjectForPOST.class);

            List<String> links = objectForPOST.getLinks();
            int linksSize = links.size();
            links.removeIf(link -> !link.matches("^(https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w\\.-]*)*\\/?(\\?.*)?$"));

            if (linksSize == links.size()) {
                statusOnly.setStatus("Ok");
            } else if (links.isEmpty()) {
                statusOnly.setStatus("Failed. No links added");
            } else {
                statusOnly.setStatus("Partially Successful");
            }

            // сохраняем линки в Redis
            if (!links.isEmpty()) {
                try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
                    jedis.connect();
                    double nowDouble = new Date().getTime();
                    String nowString = String.valueOf((long) nowDouble);

                    // Создаём в Redis список (list) с ключом текущая-дата-в-миллисекундах
                    // и значениями-линками
                    for (String link : links) {
                        jedis.rpush(nowString, link);
                    }

                    // Добавляем в сортированое множество (sorted set) funbox
                    // значение у которого score и member = текущая-дата-в-миллисекундах.
                    // В методе handleGET по score будем выполнять поиск диапазона,
                    // а массив member будет списком списков, из которых будем добывать линки
                    jedis.zadd("funbox", nowDouble, nowString);

                    // Здесь можно внести задержку 1 мс на всякий случай, чтобы точно не было одного
                    // ключа для нескольких запросов

                } catch (Exception e) {
                    statusOnly.setStatus("Failed. Can't save links: " + e);
                }
            }

        } catch (JsonParseException | JsonMappingException e) {
            System.out.println(e);
            statusOnly.setStatus("Incorrect Data");
        }

// возвращаем статус в виде ObjectStatusOnly -> JSON
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, statusOnly);

        return writer.toString();
    }


}


//     «Вытягиваем» домен из URL-адреса.
//     /https?:\/\/(?:[-\w]+\.)?([-\w]+)\.\w+(?:\.\w+)?\/?.*/i

// ^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w\.-]*)*\/?$