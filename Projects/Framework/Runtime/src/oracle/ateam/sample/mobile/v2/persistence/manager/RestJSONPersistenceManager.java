 /*******************************************************************************
  Copyright ? 2015, Oracle and/or its affiliates. All rights reserved.
  
  $revision_history$
  24-mar-2015   Steven Davelaar
  1.2           removed deletion of local rows from handleResponse method. Now done in findAll, findAllInParent methods
                in RestPersistenceManager
  19-mar-2015   Steven Davelaar
  1.1           Modified substituteNullValuesInPayload to handle MCS null value notation
  08-jan-2015   Steven Davelaar
  1.0           initial creation
 ******************************************************************************/
package oracle.ateam.sample.mobile.v2.persistence.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import oracle.adfmf.framework.api.JSONBeanSerializationHelper;
import oracle.adfmf.framework.exception.AdfException;
import oracle.adfmf.json.JSONArray;
import oracle.adfmf.json.JSONException;
import oracle.adfmf.json.JSONObject;

import oracle.ateam.sample.mobile.util.ADFMobileLogger;
import oracle.ateam.sample.mobile.util.StringUtils;
import oracle.ateam.sample.mobile.v2.persistence.db.BindParamInfo;
import oracle.ateam.sample.mobile.v2.persistence.metadata.AttributeMapping;
import oracle.ateam.sample.mobile.v2.persistence.metadata.AttributeMappingOneToMany;
import oracle.ateam.sample.mobile.v2.persistence.metadata.ClassMappingDescriptor;
import oracle.ateam.sample.mobile.v2.persistence.model.Entity;


/**
 * Implementation of Persistence manager interface that provides basic CRUD operations using
 * the REST web services protocol against a remote server. The payload of the REST web service should
 * be in JSON format.
 */
public class RestJSONPersistenceManager
  extends RestPersistenceManager
{
  private static ADFMobileLogger sLog = ADFMobileLogger.createLogger(RestJSONPersistenceManager.class);
  // overridden Rest null value, see also substituteNullValuesInPayload

  public RestJSONPersistenceManager()
  {
  }

  /**
   * See getSerializedDataObject method with attributesToExclude parameter.
   * @param entity
   * @param elementName
   * @param rowElementName
   * @param deleteRow
   * @return
   */
  protected String getSerializedDataObject(Entity entity, String elementName, String rowElementName, boolean deleteRow)
  {
    return getSerializedDataObject(entity, elementName, rowElementName,null,deleteRow);
  }

  /**
   * Returns the entity instance as JSON-formatted string. If the elementName argument has a value, a JSON Object with one attribute -the element name- is
   * returned, and the value of this attribute is a JSON array which contains one entry: the serialized entity instance. Exception to this rule is when the
   * elementName is set to "root", then the JSON array with the serailized entity instance is returned directly.
   * If the elementName is null, then the serialized data object is returned directy, and not enclosed with JSONArray brackets.
   * If the rowElementName is specified the serialized entity instance is contained by a JSON object with one attribute: the value of rowElementName.
   * You can use dots in the rowElementName to get a nested structure of JSON objects with the leaf object holding the entity instance.
   * Attributes included in the attributesToExclude list will not be serialized.
   * @param entity
   * @param elementName
   * @param rowElementName
   * @param attributesToExclude
   * @param deleteRow
   * @return JSON-formatted string containing the serialized entity instance
   */
  protected String getSerializedDataObject(Entity entity, String elementName, String rowElementName, List<String> attributesToExclude, boolean deleteRow)
  {
    Map<String,Object> keyValuePairs = getPayloadKeyValuePairs(entity,attributesToExclude);
    Map<String,Object> entityInstance = null;
    if (rowElementName != null)
    {
      String[] elements = StringUtils.stringToStringArray(rowElementName, ".");
      Map<String,Object> row = null;
      // start with last element
      for (int i = elements.length - 1; i >= 0; i--)
      {
        String element = elements[i];
        if (row == null)
        {
          row = new HashMap<String,Object>();
          row.put(element, keyValuePairs);
        }
        else
        {
          HashMap<String,Object> container = new HashMap<String,Object>();
          container.put(element, row);
          row = container;
        }
      }
      entityInstance = row;
    }
    else
    {
      entityInstance = keyValuePairs;
    }
    String json = "";
    try
    {
      if (elementName != null && !"".equals(elementName.trim()))
      {
        List<Map<String,Object>> rows = new ArrayList<Map<String,Object>>();
        rows.add(entityInstance);
        if ("root".equals(elementName))
        {
          // payload starts with array notation
          json = JSONBeanSerializationHelper.toJSON(rows).toString();
        }
        else
        {
          Map<String,Object> root = new HashMap<String,Object>();
          root.put(elementName, rows);
          json = JSONBeanSerializationHelper.toJSON(root).toString();
        }
      }
      else
      {
        json = JSONBeanSerializationHelper.toJSON(entityInstance).toString();
      }
      if (getRestNullValue() == null)
      {
        json = substituteNullValuesInPayload(json);
      }
    }
    catch (Exception e)
    {
      throw new AdfException("Error converting to JSON: " + e.getLocalizedMessage(), AdfException.ERROR);
    }
    return json;
  }

  /**
   * This method can be used to substitute parts of the json payload before it is processed, like
   * specific notations of null values.
   * It includes a work around for MAF bug 18523199, replacing the {".null":true} with null.
   * It also replaces {"@nil": "true"} with null, which is used by MCS to indicate null values.
   * Override this method if you need another null notation or want to do some other preprocessing.
   * @param json
   * @return
   */
  protected String substituteNullValuesInPayload(String json)
  {
    String newJson = StringUtils.substitute(json, "{\".null\":true}", "null");
    newJson = StringUtils.substitute(newJson, "{\"@nil\":\"true\"}", "null");
    return newJson;
  }

  /**
   * Handle JSON response payload from read methods. 
   * @param <E>
   * @param jsonResponse
   * @param entityClass
   * @param collectionElementName
   * @param rowElementName
   * @param parentBindParamInfos
   * @param deleteAllRows no longer used (delete performed in calling method)
   * @return
   */
  protected <E extends Entity> List<E> handleReadResponse(String jsonResponse, Class entityClass, String collectionElementName,
                                    String rowElementName, List<BindParamInfo> parentBindParamInfos, boolean deleteAllRows)
  {
    return handleResponse(jsonResponse, entityClass,collectionElementName,
                                    rowElementName,parentBindParamInfos, null, deleteAllRows);
  }

  /**
   * Process JSON response payload for all REST resources called through a CRUD or custom method as configured in
   * persistence-mapping.xml
   * @param <E>
   * @param json
   * @param entityClass
   * @param collectionElementName
   * @param rowElementName
   * @param parentBindParamInfos
   * @param currentEntity
   * @param deleteAllRows no longer used (delete performed in calling method)
   * @return
   */
  protected <E extends Entity> List<E> handleResponse(String json, Class entityClass, String collectionElementName,
                                    String rowElementName, List<BindParamInfo> parentBindParamInfos, E currentEntity, boolean deleteAllRows)
  {
    String jsonResponse = substituteNullValuesInPayload(json); 
    List<E> entities = new ArrayList<E>();
    if (!jsonResponse.startsWith("{") && !jsonResponse.startsWith("["))
    {
      return entities;
    }
    boolean startsAsArray = false;
    if (jsonResponse.startsWith("["))
    {
      startsAsArray = true;
      // Starts directly with list, this is currently not suported by JSONBeanSerializationHelper, so we
      // surround the list with collectionElementName object
      jsonResponse = "{\"" + collectionElementName + "\":" + jsonResponse + "}";
    }
    try
    {
      JSONObject topNode = (JSONObject) JSONBeanSerializationHelper.fromJSON(JSONObject.class, jsonResponse);
      if ("root".equals(collectionElementName) && !startsAsArray)
      {
        //        synchronizeWithLocalDB(rowElementName, topNode,entityClass,parentBindParamInfos,deleteAllRows);
        findAndProcessPayloadElements(rowElementName, topNode, entityClass, parentBindParamInfos, entities, currentEntity);
      }
      else
      {
        // collection can be JSONObject or JSONArray
        // if collectionName is null, return the topNode
        Object collection = collectionElementName!= null ? findCollectionElement(topNode, collectionElementName) :topNode;
        if (collection != null)
        {
          findAndProcessPayloadElements(rowElementName, collection, entityClass, parentBindParamInfos, entities, currentEntity);
        }
      }
    }
    catch (Exception e)
    {
//     Throw exception instead of call to MessageUtils, this allows us to catch the exception in calling method
//       and to decide there where user should see error message
//      MessageUtils.handleError(e.getLocalizedMessage());
      throw new AdfException(e);
    }
    return entities;
  }

  protected Object findCollectionElement(JSONObject node, String collectionElementName)
    throws JSONException
  {
    Object collection = null;
    Iterator keys = node.keys();
    while (keys.hasNext())
    {
      Object key = keys.next();
      Object value = node.get(key.toString());
      if (collectionElementName.equals(key))
      {
        collection = value;
        break;
      }
      else if (value instanceof JSONObject)
      {
        // do recursive call
        collection = findCollectionElement((JSONObject) value, collectionElementName);
        if (collection != null)
        {
          break;
        }
      }
    }
    return collection;
  }


  protected <E extends Entity> void findAndProcessPayloadElements(String rowElementName, Object collection, Class entityClass,
                                               List<BindParamInfo> parentBindParamInfos, List<E> entities, E currentEntity)
    throws JSONException
  {
    if (collection instanceof JSONArray)
    {
      JSONArray rows = (JSONArray) collection;
      for (int i = 0; i < rows.length(); i++)
      {
        JSONObject row = (JSONObject) rows.get(i);
        if (rowElementName != null)
        {
          Object realRow = getJSONElement(row, rowElementName, true);
          if (realRow == null)
          {
            sLog.severe("Cannot find row element name " + rowElementName);
            //              MessageUtils.handleError("Cannot find row element name " + rowElementName);
            continue;
          }
          else if (realRow instanceof JSONObject)
          {
            row = (JSONObject) realRow;
          }
          else if (realRow instanceof JSONArray)
          {
            // this can happen when the payload returns an array of objects where the objects don't have attributes
            // themselves, but only one or more child collections. The rowElementName is then not really pointing to
            // a row, but to a child array. One example of this structure is the Webcenter UCM Rest-json API: You can
            // do query which returns an array of "entry" instances. Each entry does not have attributes, only child
            // arrays like "items", or "anyOther". So, in this case we do a recursive call to process all child rows
            findAndProcessPayloadElements(null, realRow, entityClass, parentBindParamInfos, entities, currentEntity);
            continue;
          }
          else
          {
            throw new AdfException("JSON row element " + rowElementName + " is not of type JSONObject or JSONArray",AdfException.ERROR);
          }
        }
        E entity = processPayloadElement(row, entityClass, parentBindParamInfos, currentEntity);
        if (entity!=null && !entities.contains(entity))
        {
          entities.add(entity);
        }
      }
    }
    else if (collection instanceof JSONObject)
    {
      E entity = processPayloadElement((JSONObject) collection, entityClass, parentBindParamInfos, currentEntity);
      if (entity!=null && !entities.contains(entity))
      {
        entities.add(entity);
      }
    }
  }

  /**
   * This method traverses a JSONObject tree to find a JSONObject or attribute value.
   * The rowElementName argument contains the path to the JSONObject or attribute using a dot to separate
   * each level in the JSONObject tree.
   * @param row
   * @param rowElementName
   * @return
   * @throws JSONException
   */
  protected Object getJSONElement(JSONObject row, String elementName, boolean throwError)
    throws JSONException
  {
    try
    {
      String[] elements = StringUtils.stringToStringArray(elementName, ".");
      JSONObject currentObject = row;
      Object value = null;
      for (int i = 0; i < elements.length; i++)
      {
        String element = elements[i];
        if (currentObject.has(element))
        {
          value = currentObject.get(element);
          if (value instanceof JSONObject)
          {
            currentObject = (JSONObject) value;
          }
        }
        else
        {
          value = null;
          sLog.severe("Cannot find JSON element " + element + " in payload");
          break;
        }
      }
      return value;
    }
    catch (JSONException e)
    {
      if (throwError)
      {
        throw e;
      }
    }
    return null;
  }

  protected <E extends Entity> E processPayloadElement(JSONObject row, Class entityClass, List<BindParamInfo> parentBindParamInfos,
                                         E currentEntity)
    throws JSONException
  {
    if (row.keys()==null  || !row.keys().hasNext()) 
    {
        //this an empty JSON Object : "{}", do not create an entity instance
        return null;
    }
    ClassMappingDescriptor descriptor = ClassMappingDescriptor.getInstance(entityClass);
    List<BindParamInfo> bindParamInfos = new ArrayList<BindParamInfo>();
    // map contains mappign as key, and a list of instances as value
    Map<AttributeMappingOneToMany,Object> oneToManyMappings = new HashMap<AttributeMappingOneToMany,Object>();
    List<AttributeMapping> attrMappings = descriptor.getAttributeMappings();
    for (AttributeMapping mapping : attrMappings)
    {
      String attrNameInPayload = mapping.getAttributeNameInPayload();
      if (attrNameInPayload == null)
      {
        continue;
      }
      Object rawValue = getJSONElement(row, attrNameInPayload, false);
      //          checkRequired(mapping, rawValue);
      if (mapping.isOneToManyMapping())
      {
        AttributeMappingOneToMany mapping1m = (AttributeMappingOneToMany) mapping;
        if (mapping1m.getAccessorMethod() != null)
        {
          // skip this mapping as it needs to fire another rest call, we want lazy loading
          // when the child list is really needed in UI
          continue;
        }
        // some restful service return an object instead of list when there is only one item in the list
        if (rawValue instanceof JSONArray || rawValue instanceof JSONObject)
        {
          oneToManyMappings.put(mapping1m, rawValue);
        }
        continue;
      }
      BindParamInfo bpInfo = createBindParamInfoFromPayloadAttribute(entityClass, mapping, rawValue);
      if (bpInfo != null)
      {
        bindParamInfos.add(bpInfo);
      }
    }
    // loop over parent-populated attributes without payload element and populate with corresponding parent attribute
    // if available in parent bindParamInfos
    addParentPopulatedBindParamInfos(entityClass, parentBindParamInfos, bindParamInfos, descriptor);
    // insert or update the row in the database.

    // get the primary key, and check the cache for existing entity instance with this key
    // if it exists, update this instance which is then always the same as currentEntity instance
    // otherwise, when currentEntity is not null, this means the PK has changed.
    E entity = createOrUpdateEntityInstance(entityClass, bindParamInfos, currentEntity);

    if (descriptor.isPersisted())
    {
      // we cannot insert a row in SQLite when a PK value is null!
      if (isAllPrimaryKeyBindParamInfosPopulated(descriptor, bindParamInfos))
      {
        DBPersistenceManager dbpm = getLocalPersistenceManager();
        dbpm.mergeRow(bindParamInfos, true);
      }
      else
      {
        sLog.severe("Cannot insert " + entity.getClass().getName() +
                    " in database, not all primary key attributes have a value");
      }
    }

    // loop over one-to-many mappings to do recursive call to process child entities
    // And pass in the parent bindParamInfos, because the child payload might not contain the attribute
    // referencing the parent as it is already sent in hierarchical format in the payload
    Iterator mappings = oneToManyMappings.keySet().iterator();
    while (mappings.hasNext())
    {
      AttributeMappingOneToMany mapping = (AttributeMappingOneToMany) mappings.next();
      Class refClass = mapping.getReferenceClassMappingDescriptor().getClazz();
      Object childCollection = oneToManyMappings.get(mapping);
      // some restful service return an object instead of list when there is only one item in the list
      int childrenCount = 0;
      boolean isArray = false;
      JSONArray children = null;
      JSONObject onlyChild = null;
      if (childCollection instanceof JSONArray)
      {
        children = (JSONArray) childCollection;
        childrenCount = children.length();
        isArray = true;
      }
      else
      {
        onlyChild = (JSONObject) childCollection;
        childrenCount = 1;
      }

      List<Entity> childEntities = new ArrayList<Entity>();
      if (childrenCount > 0)
      {
        List<Entity> currentChildEntities = null;
        if (currentEntity != null)
        {
          currentChildEntities = (List<Entity>) currentEntity.getAttributeValue(mapping.getAttributeName());
          if (currentChildEntities.size() != childrenCount)
          {
            // this should never happen, because current entity child list is send as payload for write action
            // and the number of rows returned by ws call should be the same, if it is not, we can no longer match
            // entities by index and we dont pass in currentChildEntity
            currentChildEntities = null;
          }
        }
        for (int i = 0; i < childrenCount; i++)
        {
          Object rawValue = isArray? children.get(i): onlyChild;
          if (rawValue instanceof JSONObject)
          {
            // recursive call to populate DB with child entity row. Note that
            // multiple child rows are NOT wrapped in own GenericType, instead each
            // child instance is just an additional attribute of type GenericType
            Entity currentChildEntity = currentChildEntities != null? currentChildEntities.get(i): null;
            Entity childEntity =
              processPayloadElement((JSONObject) rawValue, refClass, bindParamInfos, currentChildEntity);
            childEntities.add(childEntity);
          }
        }
        if (childEntities.size() > 0)
        {
          entity.setAttributeValue(mapping.getAttributeName(), childEntities);
        }
      }

    }
    return entity;
  }

}
