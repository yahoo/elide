package example.models.lifecycle;

import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.core.lifecycle.LifeCycleHook;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;

import java.util.Optional;

public class OrderTwoHook implements LifeCycleHook<HookOrder> {
    @Override
    public void execute(LifeCycleHookBinding.Operation operation, LifeCycleHookBinding.TransactionPhase phase, HookOrder elideEntity, RequestScope requestScope, Optional<ChangeSpec> changes) {

    }

    @Override
    public int hashCode() {
        return 2;
    }
}
