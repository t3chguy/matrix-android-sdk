package org.matrix.matrixandroidsdk.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.data.RoomState;
import org.matrix.androidsdk.listeners.IMXEventListener;
import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.rest.model.User;
import org.matrix.matrixandroidsdk.R;
import org.matrix.matrixandroidsdk.adapters.MessagesAdapter;

import java.util.Collection;
import java.util.List;

/**
 * UI Fragment containing matrix messages for a given room.
 * Contains {@link MatrixMessagesFragment} as a nested fragment to do the work.
 */
public class MatrixMessageListFragment extends Fragment implements IMXEventListener {
    public static final String ARG_ROOM_ID = "org.matrix.matrixandroidsdk.fragments.MatrixMessageListFragment.ARG_ROOM_ID";
    public static final String ARG_LAYOUT_ID = "org.matrix.matrixandroidsdk.fragments.MatrixMessageListFragment.ARG_LAYOUT_ID";

    private static final String TAG_FRAGMENT_MATRIX_MESSAGES = "org.matrix.androidsdk.RoomActivity.TAG_FRAGMENT_MATRIX_MESSAGES";

    public static MatrixMessageListFragment newInstance(String roomId) {
        return newInstance(roomId, R.layout.fragment_matrix_message_list_fragment);
    }

    public static MatrixMessageListFragment newInstance(String roomId, int layoutResId) {
        MatrixMessageListFragment f = new MatrixMessageListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        args.putInt(ARG_LAYOUT_ID, layoutResId);
        f.setArguments(args);
        return f;
    }

    public interface MatrixMessageListListener {

    }

    private MatrixMessageListListener mMatrixMessageListListener;
    private MatrixMessagesFragment mMatrixMessagesFragment;
    private MessagesAdapter mAdapter;
    private ListView mMessageListView;
    private Handler mUiHandler;
    private String mRoomId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        // for dispatching data to add to the adapter we need to be on the main thread
        mUiHandler = new Handler(Looper.getMainLooper());

        Bundle args = getArguments();
        mRoomId = args.getString(ARG_ROOM_ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Bundle args = getArguments();
        View v = inflater.inflate(args.getInt(ARG_LAYOUT_ID), container, false);
        mMessageListView = ((ListView)v.findViewById(R.id.listView_messages));
        if (mAdapter == null) {
            // only init the adapter if it wasn't before, so we can preserve messages/position.
            mAdapter = new MessagesAdapter(getActivity(),
                    R.layout.adapter_item_messages,
                    R.layout.adapter_item_images,
                    R.layout.adapter_item_message_notice,
                    R.layout.adapter_item_message_emote
            );
        }
        mMessageListView.setAdapter(mAdapter);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle args = getArguments();
        FragmentManager fm = getActivity().getSupportFragmentManager();
        mMatrixMessagesFragment = (MatrixMessagesFragment) fm.findFragmentByTag(TAG_FRAGMENT_MATRIX_MESSAGES);

        if (mMatrixMessagesFragment == null) {
            // this fragment controls all the logic for handling messages / API calls
            mMatrixMessagesFragment = MatrixMessagesFragment.newInstance(args.getString(ARG_ROOM_ID), this);
            fm.beginTransaction().add(mMatrixMessagesFragment, TAG_FRAGMENT_MATRIX_MESSAGES).commit();
        }
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mMatrixMessageListListener = (MatrixMessageListListener)activity;
        }
        catch (ClassCastException e) {
            throw new ClassCastException("Activity "+activity+" must implement MatrixMessageListListener");
        }
    }

    public void sendMessage(String body) {
        mMatrixMessagesFragment.sendMessage(body);
    }

    public void sendEmote(String emote) {
        mMatrixMessagesFragment.sendEmote(emote);
    }

    public void requestPagination() {
        mMatrixMessagesFragment.requestPagination();
    }

    @Override
    public void onPresenceUpdate(Event event, User user) {

    }

    @Override
    public void onLiveEvent(final Event event, RoomState roomState) {
        if (mRoomId.equals(event.roomId)) {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    mAdapter.add(event);
                    // scroll to bottom
                    mMessageListView.setSelection(mMessageListView.getCount() - 1);
                }
            });
        }
    }

    @Override
    public void onBackEvent(final Event event, RoomState roomState) {
        if (mRoomId.equals(event.roomId)) {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    mAdapter.addToFront(event);
                }
            });
        }
    }

    @Override
    public void onInitialSyncComplete() {

    }

    @Override
    public void onInvitedToRoom(Room room) {

    }
}
