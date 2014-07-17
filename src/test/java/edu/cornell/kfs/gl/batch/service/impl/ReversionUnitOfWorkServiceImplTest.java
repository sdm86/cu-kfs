package edu.cornell.kfs.gl.batch.service.impl;

import org.kuali.kfs.sys.ConfigureContext;
import org.kuali.kfs.sys.context.KualiTestBase;
import org.kuali.kfs.sys.context.SpringContext;

import edu.cornell.kfs.gl.batch.service.ReversionUnitOfWorkService;
import edu.cornell.kfs.gl.businessobject.ReversionUnitOfWork;

@ConfigureContext
public class ReversionUnitOfWorkServiceImplTest extends KualiTestBase {
	
	private ReversionUnitOfWorkService accountReversionUnitOfWorkService;
	
	@Override
    protected void setUp() throws Exception {
        super.setUp();
        accountReversionUnitOfWorkService = SpringContext.getBean(ReversionUnitOfWorkService.class, "glAcctReversionUnitOfWorkService");

    }
	
	public void testLoadCategories(){
		
		ReversionUnitOfWork unitOfWork = new ReversionUnitOfWork("IT", "1003000", "-----");
		accountReversionUnitOfWorkService.loadCategories(unitOfWork);

		assertEquals(true, unitOfWork.getCategoryAmounts().size() > 0);
		
	}

}
