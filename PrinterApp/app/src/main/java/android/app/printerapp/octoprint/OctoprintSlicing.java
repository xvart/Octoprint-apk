package android.app.printerapp.octoprint;

import android.app.ProgressDialog;
import android.app.printerapp.Log;
import android.app.printerapp.MainActivity;
import android.app.printerapp.R;
import android.app.printerapp.devices.database.DatabaseController;
import android.app.printerapp.model.ModelPrinter;
import android.app.printerapp.model.ModelProfile;
import android.app.printerapp.viewer.ViewerMainFragment;
import android.content.Context;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;

public class OctoprintSlicing {

    /**
     * Upload a profile to the server with custom parameters
     * @param context
     * @param p
     * @param profile
     */
	public static void sendProfile(final Context context, final ModelPrinter p, JSONObject profile){

        StringEntity entity = null;
        String key = null;

        try {
            entity = new StringEntity(profile.toString(), "UTF-8");
            key = profile.getString("key");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }


        //Progress dialog to notify command events
		final ProgressDialog pd = new ProgressDialog(context);
		pd.setMessage(context.getString(R.string.devices_command_waiting));
		pd.show();

		
		HttpClientHandler.put(context, p.getAddress() + HttpUtils.URL_SLICING + "/" + key,
                entity, "application/json", new JsonHttpResponseHandler() {

                    @Override
                    public void onProgress(int bytesWritten, int totalSize) {
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers,
                                          JSONObject response) {
                        super.onSuccess(statusCode, headers, response);

                        Log.i("OUT", response.toString());
                        //Dismiss progress dialog
                        pd.dismiss();

                        //Reload profiles
                        retrieveProfiles(context, p);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject js) {
                        Log.i("OUT", js.toString());
                        //Dismiss progress dialog
                        pd.dismiss();
                        MainActivity.showDialog(js.toString());
                    }
                });
		
	
	}

    public static void patchProfile(final Context context, final ModelPrinter p, JSONObject profile){

        StringEntity entity = null;
        String key = null;

        try {
            entity = new StringEntity(profile.toString(), "UTF-8");
            key = profile.getString("key");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }


        //Progress dialog to notify command events
        final ProgressDialog pd = new ProgressDialog(context);
        pd.setMessage(context.getString(R.string.devices_command_waiting));
        pd.show();


        HttpClientHandler.patch(context, p.getAddress() + HttpUtils.URL_SLICING + "/" + key,
                entity, "application/json", new JsonHttpResponseHandler() {

                    @Override
                    public void onProgress(int bytesWritten, int totalSize) {
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers,
                                          JSONObject response) {
                        super.onSuccess(statusCode, headers, response);

                        Log.i("OUT", response.toString());
                        //Dismiss progress dialog

                        //Reload profiles
                        retrieveProfiles(context, p);
                        pd.dismiss();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject js) {
                        Log.i("OUT", js.toString());
                        //Dismiss progress dialog
                        pd.dismiss();
                        MainActivity.showDialog(js.toString());
                    }
                });
    }

    /**
     * Delete the profile selected by the profile parameter
     * @param context
     * @param profile
     */
    public static void deleteProfile(final Context context, final ModelPrinter p, String profile){

        HttpClientHandler.delete(context, p.getAddress() + HttpUtils.URL_SLICING + "/" + profile, new JsonHttpResponseHandler() {

            @Override
            public void onProgress(int bytesWritten, int totalSize) {
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers,
                                  JSONObject response) {
                super.onSuccess(statusCode, headers, response);

                //Reload profiles
                retrieveProfiles(context, p);

            }


        });

    }

//lkj more fast to get jason

	public static void retrieveProfiles(final Context context, final ModelPrinter p){

        ModelProfile.reloadQualityList(context);
        ArrayList<String> slicefileArrayList = ModelProfile.getQualityList();

        p.getSliceProfiles().clear();

        for (String current : slicefileArrayList){
            Log.i("lkj-retrieveProfiles", "get remote slcie profile=" + current);
            HttpClientHandler.get(p.getAddress() + HttpUtils.URL_SLICING + "/" + current, null, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers,
                                      JSONObject response) {
                    super.onSuccess(statusCode, headers, response);


                    for (JSONObject o : p.getSliceProfiles()) {

                        try {
                            if (o.getString("key").equals(response.getString("key")))
                                return;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    if (!p.getSliceProfiles().contains(response)) {
                        p.addSliceProfiles(response);
                        Log.i("lkj OctoprintSlicing retrieveProfiles", "Adding slicing profile size " + p.getSliceProfiles().size());
                        Log.i("lkj OctoprintSlicing retrieveProfiles", "Adding slicing profile:" + response);
                    }


                }


            });
        }
	}

    /**
     * Method to retrieve slice profiles before sending the file to the actual printer
     *
     */
    public static void retrieveProfiles2(final Context context, final ModelPrinter p){

        HttpClientHandler.get(p.getAddress() + HttpUtils.URL_SLICING, null, new JsonHttpResponseHandler() {

            @Override
            public void onProgress(int bytesWritten, int totalSize) {

            }

            @Override
            public void onSuccess(int statusCode, Header[] headers,
                                  JSONObject response) {
                super.onSuccess(statusCode, headers, response);

                //lkj p.getSliceProfiles().clear();

                Iterator<String> keys = response.keys();

                while (keys.hasNext()) {

                    String current = keys.next();

                    try {
                        String defProfile = response.getJSONObject(current).getString("default");

                        Log.i("lkj-retrieveProfiles", "is default slcie profile " + defProfile);
                        //if (response.getJSONObject(current).getBoolean("default")) {
                        //   Log.i("OUT", "Selected item is " + response.getJSONObject(current).getString("key"));
                        //}

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    HttpClientHandler.get(p.getAddress() + HttpUtils.URL_SLICING + "/" + current, null, new JsonHttpResponseHandler() {


                        @Override
                        public void onSuccess(int statusCode, Header[] headers,
                                              JSONObject response) {
                            super.onSuccess(statusCode, headers, response);

                            /**
                             * Check if the profile is already added because auto-refresh
                             */
                            for (JSONObject o : p.getSliceProfiles()) {

                                try {
                                    if (o.getString("key").equals(response.getString("key")))
                                        return;
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            }

                            if (!p.getSliceProfiles().contains(response)) {
                                p.addSliceProfiles(response);
                                Log.i("lkj OctoprintSlicing retrieveProfiles", "Adding slicing profile size " + p.getSliceProfiles().size());
                                Log.i("lkj OctoprintSlicing retrieveProfiles", "Adding slicing profile:" + response);
                            }


                        }


                    });

                }


            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject js) {
                Log.i("OUT", js.toString());
            }
        });

    }

    public static void getMetadata(String url, String filename){


        HttpClientHandler.get(url + HttpUtils.URL_FILES + "/local/" + filename, null, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);

                Log.i("Metadata", response.toString());

                try {

                    String estimated = response.getJSONObject("gcodeAnalysis").getString("estimatedPrintTime");
                    ViewerMainFragment.showProgressBar(StateUtils.SLICER_DOWNLOAD, Integer.parseInt(estimated));


                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        });


    }


    /**
     * Send a slice command by uploading the file first and then send the command, the result
     * will be handled in the socket payload response
     * @param context
     * @param url
     * @param file
     */
	public static void sliceCommand(final Context context, final String url, final File file, final JSONObject extras){

        RequestParams params = new RequestParams();
        try {
            params.put("file", file);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        //Log.i("Slicer","Upaload " + file.getName());
        if (file!=null)
        HttpClientHandler.post(url + HttpUtils.URL_FILES + "/local",
                params, new JsonHttpResponseHandler(){

                    //Override onProgress because it's faulty
                    @Override
                    public void onProgress(int bytesWritten, int totalSize) {
                        super.onProgress(bytesWritten, totalSize);

                        int progress = ( bytesWritten * 100 ) / totalSize;

                      //lkj  if (DatabaseController.getPreference(DatabaseController.TAG_SLICING, "Last")!=null)
                        //lkj      if ((DatabaseController.getPreference("Slicing","Last")).equals(file.getName())){
                                ViewerMainFragment.showProgressBar(StateUtils.SLICER_UPLOAD, progress);
                        //lkj    } //else sendFailureMessage(0, null, null, null);


                    }



                    //If success, the file was uploaded correctly
                    @Override
                    public void onSuccess(int statusCode, Header[] headers,
                                          JSONObject response) {
                        super.onSuccess(statusCode, headers, response);


                        Log.i("lkj Slicer", "Upload STL successful"); //TODO

                        JSONObject object = extras ;
                        StringEntity entity = null;

                        try {
                            object.put("command", "slice");
                            object.put("slicer", "cura");

                            //TODO select profile

                           // object.put("profile", profile);
                            object.put("gcode", file.getName() + "_slice.gco");
                            entity = new StringEntity(object.toString(), "UTF-8");

                            Log.i("OUT", "Uploading " + object.toString());

                        } catch (JSONException e) {		e.printStackTrace();
                        } catch (UnsupportedEncodingException e) {	e.printStackTrace();
                        }



                        Log.i("lkj Slicer", "Send slice command for " + file.getName());

                        //lkj if (DatabaseController.getPreference(DatabaseController.TAG_SLICING, "Last")!=null)
                        //lkj if ((DatabaseController.getPreference("Slicing","Last")).equals(file.getName()))
                        HttpClientHandler.post(context,url + HttpUtils.URL_FILES + "/local/" + file.getName(),
                                entity, "application/json", new JsonHttpResponseHandler(){

                                    @Override
                                    public void onProgress(int bytesWritten,
                                                           int totalSize) {
                                    }

                                    @Override
                                    public void onSuccess(int statusCode,
                                                          Header[] headers, JSONObject response) {
                                        super.onSuccess(statusCode, headers, response);


                                        ViewerMainFragment.showProgressBar(StateUtils.SLICER_SLICE, 0);
                                        Log.i("Slicer", "Slicing started");
                                    }

                                    @Override
                                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject js){
                                        Log.i("OUT", js.toString());

                                        ViewerMainFragment.showProgressBar(StateUtils.SLICER_HIDE, -1);
                                    }
                                });

                    }

                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable){
                        Log.d("lkj-Slicer", "FAILURE SLICING");
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject js){
                        Log.i("Slicer", "FAILURESLICING");
                        ViewerMainFragment.showProgressBar(StateUtils.SLICER_HIDE, -1);
                    }
                });
	}
}