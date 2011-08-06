package com.liwen.calllocate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.net.Uri;
import android.util.Log;

public class CallLocateProvider extends ContentProvider {

	public static final  String CONTENT_URI = "content://com.liwen.callocate/callocate";
	public static final String DBINFO_URI = "content://com.liwen.callocate/dbinfo";
	private static final UriMatcher mUriMatcher;
	private static final String TAG = "CallLocateProvider";
	public static final boolean DBG = false;
	private CallLocateDBHelper mDBHelper;
	private SQLiteDatabase mDB;

	private static final int CALLLOCATE_SINGLE_ROW = 1;
	private static final int CALLLOCATE_MULTI_ROW = 2;
	private static final int DBINFO_SINGLE_ROW = 3;
	private static final int DBINFO_MULTI_ROW = 4;
	
	private static final Uri _CONTENT_URI = Uri.parse(CONTENT_URI);
	private static final Uri _DBINFO_URI = Uri.parse(DBINFO_URI);
	
	private static final String CALLLOCATE_TABLE_NAME = "CallerLocView";
	private static final String AREA_TABLE_NAME = "AreaInfo";
	private static final String DBINFO_TABLE_NAME = "DBInfo";	

	public static final String DB_NAME = "calllocate.db";
	public static final String COL_NUMBER = "number";
	public static final String COL_TYPE = "type";
	public static final String COL_CODE = "code";
	public static final String COL_LOCATION = "location";
	public static final String COL_POST = "post";
	public static final String COL_AREA = "area";
	public static final String COL_REGEX = "regex";
	public static final String COL_IGNORE = "ignore";
	public static final String COL_INCEPTION = "inception";
	public static final String COL_LAST_UPDATE = "LastUpdateDate";
	public static final String COL_RECORD_COUNT = "RecordCount";
	
	static {
		mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		mUriMatcher.addURI("com.liwen.callocate", "callocate/#",
				CALLLOCATE_SINGLE_ROW);
		mUriMatcher.addURI("com.liwen.callocate", "callocate",
				CALLLOCATE_MULTI_ROW);
		mUriMatcher.addURI("com.liwen.callocate", "dbinfo/#",
				DBINFO_SINGLE_ROW);
		mUriMatcher.addURI("com.liwen.callocate", "dbinfo",
				DBINFO_MULTI_ROW);		
	}

	@Override
	public boolean onCreate() {
		mDBHelper = new CallLocateDBHelper(getContext(), DB_NAME, null, 3);
		mDB = mDBHelper.getReadableDatabase();
		return mDBHelper != null ? true : false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Cursor areas = null;
		Pattern pattern = null;
		Cursor contents = null;
		Cursor dbinfo = null;
		
		switch (mUriMatcher.match(uri)) {

			case CALLLOCATE_SINGLE_ROW: {
				try {
					String phoneNumber = uri.getPathSegments().get(1);
					if (DBG)
						Log.d(TAG, "calllocate query phone number: " + phoneNumber);
		
					if (null == phoneNumber || phoneNumber.equalsIgnoreCase(""))
						return null;
		
					areas = mDB.query(AREA_TABLE_NAME, new String[] {
							CallLocateProvider.COL_AREA,
							CallLocateProvider.COL_CODE,
							CallLocateProvider.COL_IGNORE,
							CallLocateProvider.COL_REGEX,
							CallLocateProvider.COL_INCEPTION }, null, null, null,
							null, null);
		
					if (null == areas || !areas.moveToFirst())
						return null;
		
					/* strip 17951 */
					do {
						IPCallStriper ipStriper = new IPCallStriper(
								areas.getString(areas
										.getColumnIndex(CallLocateProvider.COL_IGNORE)));
		
						PhoneNumberIncepter prefixIncepter = new PhoneNumberIncepter(
								areas.getString(areas
										.getColumnIndex(CallLocateProvider.COL_INCEPTION)));
						try {
							pattern = Pattern.compile(areas.getString(areas
									.getColumnIndex(CallLocateProvider.COL_REGEX)));
						} catch (PatternSyntaxException e) {
							continue;
						}
		
						String numberStriped = ipStriper.strip(phoneNumber);
		
						Matcher matcher = pattern.matcher(numberStriped);
		
						if (null == matcher)
							continue;
		
						String numberPrefix = null;
		
						if (matcher.find()) {
							numberPrefix = prefixIncepter.incepte(numberStriped);
						}
		
						if (null != numberPrefix && !numberPrefix.equalsIgnoreCase("")) {
							/* find calllocate of number prefix */
							String where = CallLocateProvider.COL_NUMBER + "="
							+ numberPrefix;
							contents = mDB.query(CALLLOCATE_TABLE_NAME,
								new String[] { CallLocateProvider.COL_NUMBER,
								CallLocateProvider.COL_TYPE,
								CallLocateProvider.COL_CODE,
								CallLocateProvider.COL_LOCATION,
								CallLocateProvider.COL_POST,
								CallLocateProvider.COL_AREA }, where,
								null, null, null, null);
							break;
						}
					} while (areas.moveToNext());
				} catch (Exception e) {
					Log.e(TAG, "query CALLLOCATE_SINGLE_ROW failed: " + e);
				}finally{
					if(areas != null) {
						areas.close();
						areas = null;
					}
				}
			}
			
			return contents;
		
			case CALLLOCATE_MULTI_ROW: {
				Log.d(TAG, "calllocate CALLLOCATE_MULTI_ROW not support");
				throw new IllegalArgumentException("Unsupported URI: " + uri);
			}
			case DBINFO_SINGLE_ROW: {
				Log.d(TAG, "calllocate DBINFO_SINGLE_ROW called");
				try {
				dbinfo = mDB.query(DBINFO_TABLE_NAME, new String[] {
						CallLocateProvider.COL_LAST_UPDATE,
						CallLocateProvider.COL_RECORD_COUNT}, null, null, null,
						null, null);
				}catch(Exception e) {
					Log.e(TAG, "query DBINFO_SINGLE_ROW failed: " + e);
				}
				return dbinfo;
			}		
			case DBINFO_MULTI_ROW:{
				Log.d(TAG, "calllocate DBINFO_MULTI_ROW not support");
				throw new IllegalArgumentException("Unsupported URI: " + uri);
			}
		}
		return null;
	}

	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		switch (mUriMatcher.match(uri)) {
		case CALLLOCATE_SINGLE_ROW:
			return "vnd.liwen.cursor.item/callocate";
		case CALLLOCATE_MULTI_ROW:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		case DBINFO_SINGLE_ROW:
			return "vnd.liwen.cursor.item/dbinfo";
		case DBINFO_MULTI_ROW:
			throw new IllegalArgumentException("Unsupported URI: " + uri);			
		}
		return null;
	}

	@Override
	public Uri insert(Uri arg0, ContentValues arg1) {
		return null;
	}

	@Override
	public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
		return 0;
	}

	static public class CallLocate {
		private String mNumber;
		private String mType;
		private String mCode;
		private String mLocation;
		private String mPost;
		private String mArea;

		private CallLocate() {

		}

		static public CallLocate buildCallLocate(Cursor cr) {
			if (null != cr && cr.moveToFirst()) {
				CallLocate cl = new CallLocate();
				cl.setNumber(cr.getString(cr
						.getColumnIndex(CallLocateProvider.COL_NUMBER)));
				cl.setType(cr.getString(cr
						.getColumnIndex(CallLocateProvider.COL_TYPE)));
				cl.setCode(cr.getString(cr
						.getColumnIndex(CallLocateProvider.COL_CODE)));
				cl.setLocation(cr.getString(cr
						.getColumnIndex(CallLocateProvider.COL_LOCATION)));
				cl.setPost(cr.getString(cr
						.getColumnIndex(CallLocateProvider.COL_POST)));
				cl.setArea(cr.getString(cr
						.getColumnIndex(CallLocateProvider.COL_AREA)));
				return cl;
			}
			return null;
		}

		public String getNumber() {
			return mNumber;
		}

		public void setNumber(String mNumber) {
			this.mNumber = mNumber;
		}

		public String getType() {
			return mType;
		}

		public void setType(String mType) {
			this.mType = mType;
		}

		public String getCode() {
			return mCode;
		}

		public void setCode(String mCode) {
			this.mCode = mCode;
		}

		public String getLocation() {
			return mLocation;
		}

		public void setLocation(String mLocation) {
			this.mLocation = mLocation;
		}

		public String getPost() {
			return mPost;
		}

		public void setPost(String mPost) {
			this.mPost = mPost;
		}

		public String getArea() {
			return mArea;
		}

		public void setArea(String mArea) {
			this.mArea = mArea;
		}
	}

	private class IPCallStriper {
		private String[] mTokens;

		public IPCallStriper(final String ignore) {
			if (null != ignore) {
				mTokens = ignore.split(";");
			}
		}

		public String strip(final String number) {
			if (null != mTokens && null != number) {
				for (String token : mTokens) {
					if (number.startsWith(token)) {
						return number.substring(token.length());
					}
				}
			}
			return number;
		}
	}

	private class PhoneNumberIncepter {
		private String[] mIncepters;

		public PhoneNumberIncepter(final String incepters) {
			if (null != incepters) {
				mIncepters = incepters.split(";");
			}
		}

		public String incepte(final String number) {
			if (null != mIncepters && null != number) {
				for (String incepter : mIncepters) {
					if (incepter.length() <= 4)
						continue;
					int P0 = Integer.parseInt(incepter.substring(0, 1));
					int P1 = Integer.parseInt(incepter.substring(2, 3));
					if (P1 == 0)
						continue;
					Pattern pattern = null;
					try {
						pattern = Pattern.compile(incepter.substring(4));
					} catch (PatternSyntaxException e) {
						continue;
					}
					Matcher matcher = pattern.matcher(number);
					if (matcher.find()) {
						String tmp = number.substring(0, P1);
						return tmp;
						// return matcher.find()?number.substring(0,P1):null;
					}

				}
			}
			return number;
		}
	}

	private class CallLocateDBHelper extends SQLiteOpenHelper {
		public CallLocateDBHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
			// TODO Auto-generated method stub

		}
	}
}
