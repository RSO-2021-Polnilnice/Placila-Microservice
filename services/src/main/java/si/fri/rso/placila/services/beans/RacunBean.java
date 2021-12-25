package si.fri.rso.placila.services.beans;

import si.fri.rso.placila.lib.Racun;
import si.fri.rso.placila.models.converters.RacunConverter;
import si.fri.rso.placila.models.entities.RacunEntity;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RequestScoped
public class RacunBean {

    private Logger log = Logger.getLogger(si.fri.rso.placila.services.beans.RacunBean.class.getName());

    @Inject
    private EntityManager em;


    /**  GET BY CUSTOMER ID **/
    public List<Racun> getRacuniByCustomerId(Integer customerId) {
        TypedQuery<RacunEntity> query = em.createNamedQuery(
                "RacunEntity.getByCustomerId", RacunEntity.class);
        query.setParameter("customerId", customerId);

        List<RacunEntity> resultList = query.getResultList();
        if (resultList.isEmpty()) {
            return  null;
        }
        return resultList.stream().map(RacunConverter::toDto).collect(Collectors.toList());
    }
    /**  GET BY RACUN ID **/
    public Racun getRacunById(Integer id) {
        TypedQuery<RacunEntity> query = em.createNamedQuery(
                "RacunEntity.getById", RacunEntity.class);
        query.setParameter("id", id);

        List<RacunEntity> resultList = query.getResultList();
        if (resultList.isEmpty()) {
            return  null;
        } else {
            RacunEntity result =  resultList.get(0);
            return RacunConverter.toDto(result);
        }
    }

    /** POST **/
    /** Create a new "racun"  **/
    public Racun createRacun(Racun r) {

        RacunEntity racunEntity = new RacunEntity();
        racunEntity.setStatus(r.getStatus());
        racunEntity.setPolnilnicaId(r.getPolnilnicaId());
        racunEntity.setTerminDateFrom(r.getTerminDateFrom());
        racunEntity.setTerminDateTo(r.getTerminDateTo());
        racunEntity.setCustomerId(r.getCustomerId());
        racunEntity.setCustomerUsername(r.getCustomerUsername());
        racunEntity.setCustomerEmail(r.getCustomerEmail());
        racunEntity.setCustomerFirstName(r.getCustomerFirstName());
        racunEntity.setCustomerLastName(r.getCustomerLastName());

        try {
            beginTx();
            em.persist(racunEntity);
            commitTx();
            // Refresh polnilnica entity so it shows latest data
            //em.refresh(RacunEntity);
        }
        catch (Exception e) {
            rollbackTx();
        }
        if (racunEntity.getId() == null) {
            throw new RuntimeException("Entity was not persisted");
        }

        return RacunConverter.toDto(racunEntity);
    }


    /** POST **/
    /** Change status of "racun"  **/
    public Racun changeRacunStatus(Integer id, String status) {

        RacunEntity racunEntity= em.find(RacunEntity.class, id);
        racunEntity.setStatus(status);

        try {
            beginTx();
            em.persist(racunEntity);
            commitTx();
            // Refresh polnilnica entity so it shows latest data
            em.refresh(racunEntity);
        }
        catch (Exception e) {
            rollbackTx();
        }
        if (racunEntity.getId() == null) {
            throw new RuntimeException("Entity was not persisted");
        }

        return RacunConverter.toDto(racunEntity);
    }

    /**  TRANSACTION METHODS **/
    private void beginTx() {
        if (!em.getTransaction().isActive()) {
            em.getTransaction().begin();
        }
    }

    private void commitTx() {
        if (em.getTransaction().isActive()) {
            em.getTransaction().commit();
        }
    }

    private void rollbackTx() {
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
    }
}
