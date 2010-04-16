package org.collectionspace.chain.csp.webui.userdetails;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UserDetailsSearchList implements WebMethod {

	private boolean search;
	private String base;
	private Map<String,String> type_to_url=new HashMap<String,String>();
	
	public UserDetailsSearchList(Record r,boolean search) {
		this.base=r.getID();
		this.search=search;
	}
		
	private JSONObject generateMiniRecord(Storage storage,String type,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject out=storage.retrieveJSON(type+"/"+csid+"");
		out.put("csid",csid);
		out.put("recordtype",type_to_url.get(type));
		return out;		
	}
	
	private JSONObject generateEntry(Storage storage,String base,String member) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		return generateMiniRecord(storage,base,member);
	}
	
	private JSONObject pathsToJSON(Storage storage,String base,String[] paths,String key) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		JSONObject out=new JSONObject();
		JSONArray members=new JSONArray();
		for(String p : paths)
			members.put(generateEntry(storage,base,p));
		out.put(key,members);
		return out;
	}
	
	private void search_or_list(Storage storage,UIRequest ui,String param) throws UIException {
		try {
			JSONObject restriction=new JSONObject();
			String key="items";
			if(param!=null) {
				restriction.put("screenName",param);
				key="results";
			}
			String[] paths=storage.getPaths(base,restriction);
			for(int i=0;i<paths.length;i++) {
				if(paths[i].startsWith(base+"/"))
					paths[i]=paths[i].substring((base+"/").length());
			}
			JSONObject resultsObject=new JSONObject();
			resultsObject = pathsToJSON(storage,base,paths,key);
			ui.sendJSONResponse(resultsObject);
		} catch (JSONException e) {
			throw new UIException("JSONException during autocompletion",e);
		} catch (ExistException e) {
			throw new UIException("ExistException during autocompletion",e);
		} catch (UnimplementedException e) {
			throw new UIException("UnimplementedException during autocompletion",e);
		} catch (UnderlyingStorageException e) {
			throw new UIException("UnderlyingStorageException during autocompletion",e);
		}			
	}
	

	public void run(Object in,String[] tail) throws UIException {
		Request q=(Request)in;
		if(search)
			search_or_list(q.getStorage(),q.getUIRequest(),q.getUIRequest().getRequestArgument("query"));
		else
			search_or_list(q.getStorage(),q.getUIRequest(),null);
	}

	public void configure(WebUI ui,Spec spec) {
		for(Record r : spec.getAllRecords()) {
			type_to_url.put(r.getID(),r.getWebURL());
		}
	}
}

