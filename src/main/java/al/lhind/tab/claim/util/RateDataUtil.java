package al.lhind.tab.claim.util;

import lombok.Data;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "rate.limit")
public class RateDataUtil {

    private int capacity;
    private int tokens;
    private int minutes;
}
