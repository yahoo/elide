package <%= groupId %>;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.audit.Slf4jLogger;
import com.yahoo.elide.datastores.hibernate5.HibernateStore;
import com.yahoo.elide.resources.DefaultOpaqueUserFunction;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class ElideResourceConfig extends ResourceConfig {
    public ElideResourceConfig() {
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                DefaultOpaqueUserFunction noUserFn = v -> null;
                bind(noUserFn)
                        .to(DefaultOpaqueUserFunction.class)
                        .named("elideUserExtractionFunction");

                SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();

                bind(new Elide(new ElideSettingsBuilder(new HibernateStore.Builder(sessionFactory).build())
                        .withAuditLogger(new Slf4jLogger())
                        .build()))
                        .to(Elide.class).named("elide");
            }
        });
    }
}
