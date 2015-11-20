package net.linoman.clipboardmq;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {
	RabbitMQCommunication rabbitMQCommunication;
	ClipboardManager clipboardManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

	    rabbitMQCommunication = new RabbitMQCommunication();
	    clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

	    /* Start RabbitMQ response */
	    rabbitMQCommunication.consumer(new Handler(new Handler.Callback() {
		    @Override
		    public boolean handleMessage(Message msg) {
			    ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("ClipboardMQ Imported text", msg.getData().getString(RabbitMQCommunication.MESSAGE)));
			    return false;
		    }
	    }));

	    clipboardManager.addPrimaryClipChangedListener(new ClipboardManager.OnPrimaryClipChangedListener() {
		    @Override
		    public void onPrimaryClipChanged() {
			    rabbitMQCommunication.publishMessage(clipboardManager.getPrimaryClip().getItemAt(0).getText().toString());
		    }
	    });

	    rabbitMQCommunication.producer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		rabbitMQCommunication.interruptSubscriberThread();
	}
}
