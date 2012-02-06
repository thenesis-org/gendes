package org.thenesis.gendes.cpu_gameboy;


/**
 * @author Guillaume
 * @author Mathieu
 */
public interface CPUListener {
    // Called when tracing.
    void onTrace(CPUEvent e);
    // Called when an illegal instruction is executed.
    void onIllegal(CPUEvent e);
}
