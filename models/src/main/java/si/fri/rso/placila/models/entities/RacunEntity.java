package si.fri.rso.placila.models.entities;

import javax.persistence.*;


@Entity
@Table(name = "racuni")
@NamedQueries(value =
        {
                @NamedQuery(name = "RacunEntity.getAll",
                        query = "SELECT racun FROM RacunEntity racun"),

                @NamedQuery(name = "RacunEntity.getById",
                        query = "SELECT racun FROM RacunEntity racun WHERE racun.id = :id"),

                @NamedQuery(name = "RacunEntity.getByCustomerId",
                        query = "SELECT racun FROM RacunEntity racun WHERE racun.customerId = :customerId")
        })

public class RacunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "customer_username", nullable = false)
    private String CustomerUsername;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "customer_first_name", nullable = false)
    private String customerFirstName;

    @Column(name = "customer_last_name", nullable = false)
    private String customerLastName;

    @Column(name = "polnilnica_id", nullable = false)
    private Integer polnilnicaId;

    @Column(name = "terminDateFrom", nullable = false)
    private Long terminDateFrom;

    @Column(name = "terminDateTo", nullable = false)
    private Long terminDateTo;

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

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getCustomerUsername() {
        return CustomerUsername;
    }

    public void setCustomerUsername(String customerUsername) {
        CustomerUsername = customerUsername;
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
}
