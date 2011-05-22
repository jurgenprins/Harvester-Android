package com.bokella.harvester;

import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import com.bokella.harvester.R;

public class Harvester extends Activity {
	protected static final String TAG = "Harvester";
	
	private ItemAdapter itemAdapter = null;
	private List<WebItem> webItems 	= null;
	private ProgressDialog progress = null;
	
	private boolean isLoading		= false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		final EditText urlText 	= (EditText) findViewById(R.id.Url01);
		final Button goButton 	= (Button) findViewById(R.id.getUrlButton);
		final ListView listView	= (ListView) findViewById( R.id.List01);  
		
		if (listView == null) {
			Log.e(TAG, "Cannot find list view.. aborting");
			return;
		}
		
		itemAdapter = new ItemAdapter(this, R.layout.row);
		listView.setAdapter( itemAdapter );
		
		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				WebItem i = (WebItem)listView.getAdapter().getItem(position);
				if ((i != null) && (i.getUrl() != null)) {
					Toast.makeText(getApplicationContext(), "Opening " + i.getUrl(), Toast.LENGTH_LONG).show();
					Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(i.getUrl()));
					startActivity(myIntent);
				}
			}
		});
		goButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view){
				loadItems(urlText.getText().toString());
			}
		});
		urlText.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View view, int keyCode, KeyEvent event){
				if (keyCode == KeyEvent.KEYCODE_ENTER){
					loadItems("http://www.flickr.com/search/?q=test");
					//loadItems(urlText.getText().toString());
					return true;
				}
			return false;
			}
		});
		
		Intent intent = getIntent(); 
		if (intent.getAction().equals(Intent.ACTION_SEND)) {
			String url = intent.getStringExtra(Intent.EXTRA_TEXT);
			if (url != null) {
				Log.i(TAG, "Received " + url.toString());
				loadItems(url.toString());
			}
		}
	}

	private Runnable displayItems = new Runnable() {
		public void run() {
            if ((webItems != null) && (webItems.size() > 0)){
            	itemAdapter.clear();
            	itemAdapter.notifyDataSetChanged();
                for(int i = 0; i < webItems.size(); i++) {
                	Log.i(TAG, "displayRes:  " + i + " item " + webItems.get(i).getTitle());
                	itemAdapter.add(webItems.get(i));
                }
            }
            progress.dismiss();
    		itemAdapter.notifyDataSetChanged();
        }
	};
	
	private void loadItems(final String url) {
		if (isLoading) return;
		
		isLoading = true;
		progress = ProgressDialog.show(this,    
	                "Please wait...", "Retrieving data ...", true);
		
		Thread t = new Thread() {
            public void run() {
            	final UrlParser urlParser = new UrlParser();
				
				try {
					webItems = urlParser.getItems(url);
				} catch (final Exception e) {
					Log.e(TAG, "loadItems: " + e.getMessage());
					isLoading = false;
					runOnUiThread(new Runnable() {
					    public void run() {
					    	progress.dismiss();
					        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
					    }
					});
					return;
				}
				
				if ((webItems == null) || (webItems.size() == 0)) {
					Log.i(TAG, "loadItems: no items found");
					isLoading = false;
					runOnUiThread(new Runnable() {
					    public void run() {
					    	progress.dismiss();
					        Toast.makeText(getApplicationContext(), "No items found", Toast.LENGTH_SHORT).show();
					    }
					});
					return;
				}
		
				Log.i(TAG, "Done loading " + webItems.size() + " items");
				isLoading = false;
				runOnUiThread(displayItems);
            }
        };
        t.start();
       
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    Log.i(TAG, "menu: creating menu");
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.create_alert:
	    	Log.i(TAG, "menu: create alert");
	        return true;
	    case R.id.list_alerts:
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	private class ItemAdapter extends ArrayAdapter<WebItem> {
		private Activity activity;
		public ImageLoader imageLoader = null;
		private LayoutInflater vi = null;
		
		public ItemAdapter(Activity activity, int textViewResourceId) {
			super(activity, textViewResourceId);
			this.activity 		= activity;
			this.imageLoader 	= new ImageLoader(activity.getApplicationContext());
			this.vi 			= (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}	
		
		@Override
        public View getView(int position, View convertView, ViewGroup parent) {
		        View v = convertView;
                if (v == null) {
                    v = vi.inflate(R.layout.row, null);
                }
            	Log.i(TAG, "getView: at position" + position + " using itemlist of " + this.getCount());
                WebItem i = this.getItem(position);
                if (i != null) {
                		Log.i(TAG, "getView: item is " + i.getTitle());
                        TextView title = (TextView) v.findViewById(R.id.title);
                        if ((title != null) && (i.getTitle() != null)) {
                        	title.setText(i.getTitle());                            
                        }
                
                        TextView url = (TextView) v.findViewById(R.id.url);
                        if ((url != null) && (i.getUrl() != null)) {
                        	url.setText(i.getUrl());                            
                        }
        
                        TextView summary = (TextView) v.findViewById(R.id.summary);
                        if ((summary != null) && (i.getSummary() != null)) {
                        	summary.setText(i.getSummary());                           
                        }
    
                        TextView status = (TextView) v.findViewById(R.id.status);
                        if ((status != null) && (i.getStatus() != null)) {
                        	status.setText(i.getStatus());
                        }
                        
                        ImageView thumb = (ImageView) v.findViewById(R.id.thumb);
                        if ((thumb != null) && (i.getThumbUrl() != null)) {
                        	Log.i(TAG, "getView: loading img " + i.getThumbUrl());
                        	thumb.setTag(i.getThumbUrl());
                        	imageLoader.DisplayImage(i.getThumbUrl(), activity, thumb);
                        }
                        
				}
                return v;
        }
	}
}
