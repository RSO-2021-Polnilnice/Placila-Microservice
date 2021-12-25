package si.fri.rso.placila.models.converters;

import si.fri.rso.placila.lib.Racun;
import si.fri.rso.placila.models.entities.RacunEntity;

public class RacunConverter {

    public static Racun toDto(RacunEntity entity) {

        Racun dto = new Racun();
        // Map entity into dto
        dto.setId(entity.getId());
        dto.setStatus(entity.getStatus());

        dto.setPolnilnicaId(entity.getPolnilnicaId());
        dto.setTerminDateFrom(entity.getTerminDateFrom());
        dto.setTerminDateTo(entity.getTerminDateTo());

        dto.setCustomerId(entity.getCustomerId());
        dto.setCustomerUsername(entity.getCustomerEmail());
        dto.setCustomerEmail(entity.getCustomerFirstName());
        dto.setCustomerLastName(entity.getCustomerLastName());
        return dto;

    }

    public static RacunEntity toEntity(Racun dto) {

        RacunEntity entity = new RacunEntity();
        // Map dto into entity
        if (dto.getId() != null) entity.setId(dto.getId());
        entity.setStatus(dto.getStatus());

        entity.setPolnilnicaId(dto.getPolnilnicaId());
        entity.setTerminDateFrom(dto.getTerminDateFrom());
        entity.setTerminDateTo(dto.getTerminDateFrom());

        entity.setCustomerId(dto.getCustomerId());
        entity.setCustomerUsername(dto.getCustomerUsername());
        entity.setCustomerEmail(dto.getCustomerEmail());
        entity.setCustomerFirstName(dto.getCustomerFirstName());
        entity.setCustomerLastName(dto.getCustomerLastName());

        return entity;

    }
}
