package com.sunoray.tentacle.layout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.webkit.WebView;

import com.sunoray.tentacle.R;

public class WebViewSwipeRefreshLayout extends SwipeRefreshLayout {
	private WebView view;
	
	public WebViewSwipeRefreshLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setView(WebView view) {
		this.view = view;
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		view = (WebView) findViewById(R.id.view_webview_view);
	}

	@SuppressLint("NewApi")
	@Override
	public boolean canChildScrollUp() {
		// refresh only can work in android 4+ (ICS)		
		if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			return true;
		else
			return view.canScrollVertically(-1);
	}
	@Override
	protected void onDraw(Canvas canvas) {		
		super.onDraw(canvas);
	}		
	
	@Override
	public void setColorSchemeColors(int... colors) {		
		super.setColorSchemeColors(new int[]{Color.rgb(0, 101, 67)});
	}

}
