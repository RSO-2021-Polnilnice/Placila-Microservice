package si.fri.rso.placila.lib;

public class Racun {

    private Integer id;
    private String status;
    // "Termin" variables
    private Integer polnilnicaId;
    private Long terminDateFrom;
    private Long terminDateTo;
    // "User" variables
    private Integer customerId;
    private String customerUsername;
    private String customerEmail;
    private String customerFirstName;
    private String customerLastName;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPolnilnicaId() {
        return polnilnicaId;
    }

    public void setPolnilnicaId(Integer polnilnicaId) {

        this.polnilnicaId = polnilnicaId;
    }

    public Long getTerminDateFrom() {
        return terminDateFrom;
    }

    public void setTerminDateFrom(Long terminDateFrom) {
        this.terminDateFrom = terminDateFrom;
    }

    public Long getTerminDateTo() {
        return terminDateTo;
    }

    public void setTerminDateTo(Long terminDateTo) {
        this.terminDateTo = terminDateTo;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getCustomerUsername() {
        return customerUsername;
    }

    public void setCustomerUsername(String customerUsername) {
        this.customerUsername = customerUsername;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerFirstName() {
        return customerFirstName;
    }

    public void setCustomerFirstName(String customerFirstName) {
        this.customerFirstName = customerFirstName;
    }

    public String getCustomerLastName() {
        return customerLastName;
    }

    public void setCustomerLastName(String customerLastName) {
        this.customerLastName = customerLastName;
    }
}
