package si.fri.rso.placila.models.converters;

import si.fri.rso.placila.lib.Racun;
import si.fri.rso.placila.models.entities.RacunEntity;

public class RacunConverter {

    public static Racun toDto(RacunEntity entity) {

        Racun dto = new Racun();
        // Map entity into dto
        dto.setId(entity.getId());
        dto.setTimestamp(entity.getTimestamp());
        dto.setStatus(entity.getStatus());
        dto.setPrice(entity.getPrice());

        dto.setPolnilnicaId(entity.getPolnilnicaId());
        dto.setTerminId(entity.getTerminId());
        dto.setTerminDateFrom(entity.getTerminDateFrom());
        dto.setTerminDateTo(entity.getTerminDateTo());

        dto.setCustomerId(entity.getCustomerId());
        dto.setCustomerUsername(entity.getCustomerUsername());
        dto.setCustomerEmail(entity.getCustomerEmail());
        dto.setCustomerLastName(entity.getCustomerLastName());
        dto.setCustomerFirstName(entity.getCustomerFirstName());
        return dto;

    }

    public static RacunEntity toEntity(Racun dto) {

        RacunEntity entity = new RacunEntity();
        // Map dto into entity
        if (dto.getId() != null) entity.setId(dto.getId());
        entity.setStatus(dto.getStatus());
        entity.setTimestamp(dto.getTimestamp());
        entity.setPrice(dto.getPrice());

        entity.setPolnilnicaId(dto.getPolnilnicaId());
        entity.setTerminDateFrom(dto.getTerminDateFrom());
        entity.setTerminDateTo(dto.getTerminDateFrom());
        entity.setTerminId(dto.getTerminId());

        entity.setCustomerId(dto.getCustomerId());
        entity.setCustomerUsername(dto.getCustomerUsername());
        entity.setCustomerEmail(dto.getCustomerEmail());
        entity.setCustomerFirstName(dto.getCustomerFirstName());
        entity.setCustomerLastName(dto.getCustomerLastName());

        return entity;

    }
}
