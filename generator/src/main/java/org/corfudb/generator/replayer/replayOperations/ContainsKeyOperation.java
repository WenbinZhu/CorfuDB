package org.corfudb.generator.replayer.replayOperations;

import org.corfudb.util.auditor.Event;

/**
 * Created by Sam Behnam on 2/15/18.
 */
public class ContainsKeyOperation extends Operation {
    public ContainsKeyOperation(Configuration configuration) {
        super(configuration);
    }

    @Override
    public Object execute(Event event) {
        return getConfiguration()
                .getMap(event.getMapId())
                .containsKey(event.getKey());
    }
}
