package org.thenesis.emulation.eclipse;


import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * This class implements the interface required by the workbench
 * for all 'New' wizards.  This wizard creates readme files.
 */
public class ReadmeCreationWizard extends Wizard implements INewWizard {
    private IStructuredSelection selection;

    private IWorkbench workbench;

    private ReadmeCreationPage mainPage;

    /** (non-Javadoc)
     * Method declared on Wizard.
     */
    public void addPages() {
        mainPage = new ReadmeCreationPage(workbench, selection);
        addPage(mainPage);
    }

    /** (non-Javadoc)
     * Method declared on IWorkbenchWizard
     */
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
        this.selection = selection;
        setWindowTitle(MessageUtil.getString("New_Readme_File")); //$NON-NLS-1$
        setDefaultPageImageDescriptor(ReadmeImages.README_WIZARD_BANNER);
    }

    /** (non-Javadoc)
     * Method declared on IWizard
     */
    public boolean performFinish() {
        return mainPage.finish();
    }
}
