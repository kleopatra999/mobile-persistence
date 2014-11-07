package oracle.ateam.sample.mobile.datasynch.model;

import java.util.List;

import oracle.adfmf.framework.api.AdfmfJavaUtilities;

import oracle.ateam.sample.mobile.persistence.service.DataSynchAction;
import oracle.ateam.sample.mobile.persistence.service.DataSynchManager;
import oracle.ateam.sample.mobile.persistence.service.EntityCRUDService;
import oracle.ateam.sample.mobile.util.MessageUtils;

public class DataSynchService extends EntityCRUDService
{
  public DataSynchService()
  {
    super();
  }
  
  public DataSynchManager getCurrentDataSynchManager()
  {
    String entityClass = (String) AdfmfJavaUtilities.evaluateELExpression("#{applicationScope.dataSynchEntity}");
    if (entityClass==null)
    {
      MessageUtils.handleError("No data synch manager entity class set!");
    }
    DataSynchManager curManager = DataSynchManager.geDataSynchManager(entityClass);    
    return curManager;
  }

  protected Class getEntityClass()
  {
    return DataSynchAction.class;
  }

  protected String getEntityListName()
  {
    return "dataSynchActions";
  }
  
  public DataSynchAction[] getDataSynchActions()
  {
    DataSynchAction[] dataObjects = (DataSynchAction[]) getEntityList().toArray(new DataSynchAction[getEntityList().size()]);
    return dataObjects;
  }

}
