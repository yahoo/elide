package example.embeddedid;

import com.yahoo.elide.annotation.Include;
import example.BaseId;
import lombok.Data;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;

@Include
@Table(name = "house")
@Data
@Entity
public class House extends BaseId {

    @Embedded
    private HouseAddress address;

    private String name;
}
