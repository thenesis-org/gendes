package org.thenesis.emulation.eclipse;


import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

/**
 * Creates resolutions for readme markers.
 */
public class ReadmeMarkerResolutionGenerator implements
        IMarkerResolutionGenerator2 {

    /* (non-Javadoc)
     * Method declared on IMarkerResolutionGenerator.
     */
    public IMarkerResolution[] getResolutions(IMarker marker) {
        return new IMarkerResolution[] { new AddSentenceResolution() };
    }

    /* (non-Javadoc)
     * Method declared on IMarkerResolutionGenerator2.
     */
    public boolean hasResolutions(IMarker marker) {
        return true;
    }

}
