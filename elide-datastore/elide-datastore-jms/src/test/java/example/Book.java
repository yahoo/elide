package example;

import com.yahoo.elide.annotation.Include;
import lombok.Data;

import javax.persistence.Id;

@Include
@Data
public class Book {
    @Id
    private long id;

    private String title;
}
