package at.nonblocking.maven.nonsnapshot;

import org.apache.maven.plugin.logging.SystemStreamLog;

public class DebugSystemStreamLog extends SystemStreamLog {

    @Override
    public boolean isDebugEnabled() {
        return true;
    }
}
