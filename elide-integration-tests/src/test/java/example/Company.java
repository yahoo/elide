package example;

import com.yahoo.elide.annotation.Include;
import lombok.Data;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Include
@Data
public class Company {

    @Id
    private String id;
    private String description;

    @Embedded
    private Address address;
}
