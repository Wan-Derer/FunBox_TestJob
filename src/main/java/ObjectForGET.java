import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.Set;

@JsonAutoDetect
public class ObjectForGET {
    private Set<String> domains;
    private String status;

    public ObjectForGET(Set<String> domains, String status) {
        this.domains = domains;
        this.status = status;
    }

    public ObjectForGET() {
    }

    public Set<String> getDomains() {
        return domains;
    }

    public String getStatus() {
        return status;
    }
}
