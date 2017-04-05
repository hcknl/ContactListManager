
import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDiskIOException;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Patterns;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class ContactListManager {

    public static InputStream getContactThumbStreamById(int contactId, ContentResolver contentResolver) {
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
        Cursor cursor = contentResolver.query(photoUri, new String[]{ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
        if (cursor == null) return null;

        try {
            if (cursor.moveToFirst()) {
                byte[] data = cursor.getBlob(0);
                if (data != null) {
                    return new ByteArrayInputStream(data);
                }
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    public static boolean createOrUpdateContactPhoto(int rawContactId, byte[] photoBytes, Context context) {
        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues();
        int photoRow = -1;
        String where = ContactsContract.Data.RAW_CONTACT_ID + " == " + rawContactId + " AND " + ContactsContract.Contacts.Data.MIMETYPE + "=='" + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'";
        Cursor cursor = cr.query(ContactsContract.Data.CONTENT_URI, null, where, null, null);
        if (cursor != null) {
            int idIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data._ID);
            if (cursor.moveToFirst()) {
                photoRow = cursor.getInt(idIdx);
            }
            cursor.close();
        }
        values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
        values.put(ContactsContract.Data.IS_SUPER_PRIMARY, 1);
        values.put(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes);
        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
        try {
            int i = 0;
            Uri newUri = null;
            if (photoRow >= 0) {
                i = cr.update(ContactsContract.Data.CONTENT_URI, values, ContactsContract.Data._ID + " = " + photoRow, null);
            } else {
                newUri = cr.insert(ContactsContract.Data.CONTENT_URI, values);
            }
            return i > 0 || newUri != null;
        } catch (SQLiteDiskIOException dIOe) {
            dIOe.printStackTrace();
        }
        return false;
    }

    public static String getContactPhotoUriById(int contactId, Context context) {

        String[] contactProjection = new String[]{ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        };

        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, contactProjection, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{String.valueOf(contactId)}, null);

        String photoThumbUri;
        if (cursor != null && cursor.getCount() > 0) {

            cursor.moveToFirst();

            int photoThumbnailUriIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI);

            photoThumbUri = cursor.getString(photoThumbnailUriIdx);

            cursor.close();

            return photoThumbUri;
        } else {
            return null;
        }
    }

    public static String getContactPhotoThumbUriById(int contactId, Context context) {

        String[] contactProjection = new String[]{ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
        };

        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, contactProjection, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{String.valueOf(contactId)}, null);

        String photoThumbUri;
        if (cursor != null && cursor.getCount() > 0) {

            cursor.moveToFirst();

            int photoThumbnailUriIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI);

            photoThumbUri = cursor.getString(photoThumbnailUriIdx);

            cursor.close();

            return photoThumbUri;
        } else {
            return null;
        }
    }

    public static String getContactDisplayNameByPhoneNumber(String phoneNumber, Context context) {

        ContentResolver cr = context.getContentResolver();
        Cursor resultCur;
        String[] projection = new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME};
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        resultCur = cr.query(uri, projection, null, null, null);
        String name;
        if (resultCur != null) {
            resultCur.moveToNext();
            name = resultCur.getString(resultCur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            resultCur.close();
            return name;
        }
        return null;
    }

    public static void getContactList(final Context context) {

        final List<ContactModel> contactListModels = new ArrayList<>();

        String[] contactProjection = new String[]{ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,

        };

        String where = ContactsContract.Contacts.HAS_PHONE_NUMBER + " > " + 0 + " AND "
                + ContactsContract.Contacts.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE + "'" + " AND "
                + ContactsContract.RawContacts.ACCOUNT_TYPE + "<>'com.whatsapp'" + " AND "
                + ContactsContract.RawContacts.ACCOUNT_TYPE + "<>'com.linkedin.android'";


        String sortOrder = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC";

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, contactProjection, where, null, sortOrder);
            if (cursor != null && cursor.getCount() > 0) {
                int contactIdIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
                int rawContactIdIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID);
                int nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int phoneNumberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                int photoUriIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI);
                int thumbUriIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI);

                cursor.moveToFirst();
                do {
                    try {
                        int contactId = cursor.getInt(contactIdIdx);
                        int PhonebookRawContactId = cursor.getInt(rawContactIdIdx);
                        String FirstName = cursor.getString(nameIdx);
                        String PhoneNumber = cursor.getString(phoneNumberIdx);
                        String ThumbnailPhotoUri = cursor.getString(thumbUriIdx);
                        String PhotoUri = cursor.getString(photoUriIdx);

                        ContactModel cm = new ContactModel();
                        cm.setPhonebookContactId(contactId);
                        cm.setPhonebookRawContactId(PhonebookRawContactId);
                        cm.setDisplayName(FirstName);
                        cm.setPhoneNumber(PhoneNumber);
                        cm.setThumbnailPhotoUri(ThumbnailPhotoUri);
                        cm.setPhotoUri(PhotoUri);

                        contactListModels.add(cm);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } while (cursor.moveToNext());

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static boolean isValidEmail(CharSequence mail) {
        return !TextUtils.isEmpty(mail) && Patterns.EMAIL_ADDRESS.matcher(mail).matches();
    }

    public static void getContactEmailList(final Context context) {
        final List<ContactModel> contactList = new ArrayList<>();

        String[] contactProjection = new String[]{ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Email.ADDRESS

        };

        String where = ContactsContract.Contacts.HAS_PHONE_NUMBER + " > " + 0 + " AND "
                + ContactsContract.Contacts.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE + "'" + " AND "
                + ContactsContract.RawContacts.ACCOUNT_TYPE + "<>'com.whatsapp'" + " AND "
                + ContactsContract.RawContacts.ACCOUNT_TYPE + "<>'com.linkedin.android'";

        String sortOrder = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC";

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, contactProjection, where, null, sortOrder);

            if (cursor != null && cursor.getCount() > 0) {

                int contactIdIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
                int emailIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS);

                cursor.moveToFirst();

                do {
                    try {
                        int contactId = cursor.getInt(contactIdIdx);
                        String mail = cursor.getString(emailIdx);

                        ContactModel c = new ContactModel();
                        c.setPhonebookContactId(contactId);

                        if (isValidEmail(mail)) {
                            c.setEmail(mail);
                            contactList.add(c);

                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static List<ContactGroup> getContactGroups(Context con) {
        Cursor cursor = null;
        List<ContactGroup> groups = new ArrayList<>();

        try {
            String[] projection = {
                    ContactsContract.Groups._ID,
                    ContactsContract.Groups.TITLE,
                    ContactsContract.Groups.ACCOUNT_NAME,
                    ContactsContract.Groups.ACCOUNT_TYPE,
            };
            int len = 0;
            cursor = con.getContentResolver().query(ContactsContract.Groups.CONTENT_URI, projection, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                len = cursor.getCount();
            }

            for (int i = 0; i < len; i++) {
                ContactGroup group = new ContactGroup();
                group.id = Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Groups._ID)));
                group.title = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.TITLE));
                group.accountName = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.ACCOUNT_NAME));
                group.accountType = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.ACCOUNT_TYPE));

                int count = getCountOfGroupMembers(group.id, con);
                if (count > 0) {
                    groups.add(group);
                }
                cursor.moveToNext();
            }
            if (cursor != null)
                cursor.close();
        } catch (Exception e) {
            if (cursor != null)
                cursor.close();
        }

        return groups;
    }

    public static List<GroupMembership> getContactGroupMembersById(int groupId, Context con) {
        List<GroupMembership> members = new ArrayList<>();

        String where = ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + "="
                + groupId + " AND "
                + ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE + "='"
                + ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE + "'";

        Cursor contactCursor = con.getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.GroupMembership.RAW_CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                }, where, null, ContactsContract.Data.DISPLAY_NAME + " COLLATE LOCALIZED ASC");


        if (contactCursor != null) {
            contactCursor.moveToFirst();
            int ContactIdColIndex = contactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);

            do {
                int ContactId = contactCursor.getInt(ContactIdColIndex);

                GroupMembership cm = new GroupMembership();
                cm.setContactId(ContactId);
                cm.setGroupId(groupId);

                members.add(cm);
            } while (contactCursor.moveToNext());
            contactCursor.close();
        }
        return members;
    }

    public static int getCountOfGroupMembers(int groupId, Context con) {
        String where = ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + "="
                + groupId + " AND "
                + ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE + "='"
                + ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE + "'";

        Cursor contactCursor = con.getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.GroupMembership.RAW_CONTACT_ID,
                        ContactsContract.Data.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                }, where, null, ContactsContract.Data.DISPLAY_NAME + " COLLATE LOCALIZED ASC");

        int memberSize = 0;
        if (contactCursor != null) {
            memberSize = contactCursor.getCount();
            contactCursor.close();
        }

        return memberSize;
    }

    public static List<CallLogModel> getCallDetails(Context context) {

        List<CallLogModel> data = new ArrayList<>();

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return data;
        }
        String[] columns = new String[]{CallLog.Calls.DURATION, CallLog.Calls.NUMBER};

        String where = CallLog.Calls.NUMBER + " IS NOT NULL " + " AND " + CallLog.Calls.DURATION + " > 0";


        Cursor managedCursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, columns, where, null, null);
        if (managedCursor == null) {
            return data;
        }

        int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
        int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);

        while (managedCursor.moveToNext()) {
            String phNumber = managedCursor.getString(number);
            int callDuration = managedCursor.getInt(duration);

            CallLogModel c = new CallLogModel();
            c.setDuration(callDuration);
            c.setPhone(phNumber);
            data.add(c);
        }
        managedCursor.close();

        if (data.size() == 0) {
            return data;
        }
        Set<CallLogModel> uniqueDatas = new HashSet<>(data);
        int sum;
        for (CallLogModel unique : uniqueDatas) {
            sum = 0;
            for (CallLogModel original : data) {
                if (unique.getPhone().equals(original.getPhone())) {
                    sum += original.getDuration();
                }
            }
            unique.setDuration(sum);
        }
        data.clear();
        data.addAll(uniqueDatas);

        Collections.sort(data, new CallLogComparator());

        return data;

    }


    public static ContactModel getContactById(int phoneBookContactId, Context context) {

        String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Photo.PHOTO_THUMBNAIL_URI
        };

        String where = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + phoneBookContactId + " AND " + ContactsContract.Contacts.HAS_PHONE_NUMBER + " > " + 0 + " AND "
                + ContactsContract.Contacts.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'" + " AND "
                + ContactsContract.RawContacts.ACCOUNT_TYPE + "<>'com.whatsapp'" + " AND "
                + ContactsContract.RawContacts.ACCOUNT_TYPE + "<>'com.linkedin.android'";

        String sortOrder = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC";

        ContactModel contact = null;

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, projection, where, null, sortOrder);
            if (cursor != null && cursor.getCount() > 0) {
                int contactIdIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
                int nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int phoneNumberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                int photoUriIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO_THUMBNAIL_URI);

                cursor.moveToFirst();

                int id = cursor.getInt(contactIdIdx);
                String displayName = cursor.getString(nameIdx);
                String phoneNumber = cursor.getString(phoneNumberIdx);

                contact = new ContactModel();

                contact.setDisplayName(displayName);
                contact.setPhoneNumber(phoneNumber);

            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return contact;

    }


}
