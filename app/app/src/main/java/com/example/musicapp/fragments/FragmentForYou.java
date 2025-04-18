package com.example.musicapp.fragments;

import io.reactivex.rxjava3.core.Observable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicapp.R;
import com.example.musicapp.adapters.ContentFragForYouAdapter;
import com.example.musicapp.models.Frame;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;


public class FragmentForYou extends Fragment {

    private RecyclerView recyclerView;
    private ContentFragForYouAdapter adapter;
    private List<Frame> frameList;
    //tổng số lượng đối tượng frame
    private int sizeOfListInDatabase = 0;
    //đếm xem có bao nhiêu đối tượng frame được tải về
    private int frameCount = 0;
    //dispoable để ngắt kết nối giữa observable và observer
    private Disposable disposable;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_foryou,container,false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView=view.findViewById(R.id.rc_FragForYou);

        frameList=new ArrayList<>();
        getSizeOfList();
        Observable<Frame> observable = getObservableContent();
        Observer<Frame> observer = getObserverContent();
        //đăng ký luồng (load dữ liệu ngầm)
        observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);

        adapter=new ContentFragForYouAdapter(frameList,getActivity());
        LinearLayoutManager linear = new LinearLayoutManager(getActivity()
                ,LinearLayoutManager.VERTICAL,false);
        recyclerView.setLayoutManager(linear);
        recyclerView.setAdapter(adapter);
    }
    //xử lý load dữ liệu từ luồng
    private Observable<Frame> getObservableContent(){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference reference = database.getReference("FrameList");

        return Observable.create(new ObservableOnSubscribe<Frame>() {
            @Override
            public void subscribe(@io.reactivex.rxjava3.annotations.NonNull ObservableEmitter<Frame> emitter) throws Throwable {
                reference.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        Frame frame = snapshot.getValue(Frame.class);
                        if(!emitter.isDisposed()){
                            if(frame != null) {
                                emitter.onNext(frame);
                                frameCount++;
                            }else{
                                emitter.onError(new Exception());
                            }
                        }
                        if(sizeOfListInDatabase==frameCount)
                            emitter.onComplete();
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });
    }

    private Observer<Frame> getObserverContent(){
        return new Observer<Frame>() {
            @Override
            public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                disposable=d;
            }

            @Override
            public void onNext(@io.reactivex.rxjava3.annotations.NonNull Frame frame) {
                frameList.add(frame);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                disposable.dispose();
            }

            @Override
            public void onComplete() {
                disposable.dispose();
            }
        };
    }
    //lấy size của dữ liệu
    private void getSizeOfList(){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference reference = database.getReference("FrameList");

        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                sizeOfListInDatabase = (int) snapshot.getChildrenCount();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }
}

