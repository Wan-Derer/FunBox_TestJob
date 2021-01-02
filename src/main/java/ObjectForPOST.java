import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.ArrayList;
import java.util.List;

@JsonAutoDetect
public class ObjectForPOST {
//    private String[] links;
    private List<String> links = new ArrayList<>();

//    public ObjectForPOST(String[] domains, String status) {
//        this.domains = domains;
//    }

    public ObjectForPOST() {
    }

    public List<String> getLinks() {
        return links;
    }

    public void setLinks(List<String> links) {
        this.links = links;
    }
}
