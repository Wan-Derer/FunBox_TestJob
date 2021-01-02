import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect
public class ObjectStatusOnly {
    private String status;

//    public ObjectStatusOnly(String status) {
//        this.status = status;
//    }

    public ObjectStatusOnly() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
