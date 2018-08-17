package example;

import com.yahoo.elide.annotation.Include;
import lombok.Setter;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.Set;

@Entity
@Include
@Audited // Ensure envers does not cause any issues
public class House {
    @Setter
    private long id;

    @Setter
    private AddressFragment address;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    @Column(name = "address", columnDefinition = "TEXT")
    @Type(type = "com.yahoo.elide.datastores.hibernate5.usertypes.JsonType", parameters = {
            @Parameter(name = "class", value = "example.AddressFragment")
    })
    public AddressFragment getAddress() {
        return address;
    }

    private Person owner;
    @ManyToOne
    public Person getOwner() {
        return owner;
    }
    public void setOwner(Person owner) {
        this.owner = owner;
    }
}
