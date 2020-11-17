package com.app.scrabblegame;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static com.app.scrabblegame.ScrabbleApp.dictionary;

public class GameActivity extends AppCompatActivity {

    private GridLayout scrabble;
    private String selectedLetter;
    private int selectedLetterId = -1;
    private String letters = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ";
    private int [] scores = {1,3,1,3,2,1,3,5,5,1,4,2,2,2,1,1,2,1,1,1,2,10,5,5,5,8,10,10,4,3,8,8,3};
    private ArrayList<Integer> selectedButtons = new ArrayList<>();
    private int[] buttonsIds = {R.id.letter1, R.id.letter2, R.id.letter3, R.id.letter4,
            R.id.letter5, R.id.letter6, R.id.letter7};
    private boolean isFirstMove = true;
    private ArrayList<GridLetter> gridLetters = new ArrayList<>();
    private ArrayList<GridLetter> gridLettersTemp = new ArrayList<>();
    private ArrayList<String> words = new ArrayList<>();
    private String sessionId;
    private AlertDialog alertDialog;
    private DatabaseReference mDatabase;
    private String userName;
    private long userId;
    private ArrayList<User> users = new ArrayList<>();
    private int lettersLeft = 104;
    private int passes = 0;
    private boolean gameOver=false;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        sessionId = getIntent().getStringExtra("sessionId");
        userName = getIntent().getStringExtra("userName");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_wait_users, null);
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setView(dialogView);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
                builder.setMessage(R.string.session_exit);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        exitSession();
                    }
                });
                builder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        alertDialog.show();
                    }
                });
                builder.show();
            }
        });
        alertDialog = dialog.create();

        getUsers();

        initUI();

    }

    private void getUsers(){
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("sessions").child(sessionId).child("users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userId = 0;
                for (DataSnapshot users: dataSnapshot.getChildren()){
                    User user = users.getValue(User.class);
                    if (user!=null){
                        if (user.getId()>=userId){
                            userId = user.getId()+1;
                        }
                    }
                }
                User userFb = new User();
                userFb.setId(userId);
                userFb.setName(userName);
                userFb.setScore(0);
                userFb.setPasses(0);
                if (userId==0){
                    userFb.setUserMove(0);
                } else {
                    userFb.setUserMove(1);
                }
                mDatabase.child("sessions").child(sessionId).child("users").child(String.valueOf(userId)).setValue(userFb);
                Intent logOutService = new Intent(GameActivity.this, LogOutService.class);
                logOutService.putExtra("sessionId", sessionId);
                logOutService.putExtra("userId", String.valueOf(userId));
                startService(logOutService);
                waitUsers();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void waitUsers(){
        mDatabase.child("sessions").child(sessionId).child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long userCount = dataSnapshot.getChildrenCount();
                if (userCount>1){
                    users.clear();
                    for (DataSnapshot user: dataSnapshot.getChildren()) {
                        User userFb = user.getValue(User.class);
                        users.add(userFb);
                        if (userFb.getName().equals(userName)){
                            currentUser = userFb;
                            if (userFb.getUserMove()==1) {
                                try {
                                    alertDialog.show();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                try {
                                    alertDialog.dismiss();
                                } catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    setUsers();
                } else {
                    if (!gameOver) {
                        try {
                            alertDialog.show();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setUsers(){
        if (users.get(0).getPasses()==2&&users.get(1).getPasses()==2){
            endGame();
        } else {
            passes = currentUser.getPasses();
            for (int i = 0; i < users.size(); i++) {
                if (users.get(i).getName().equals(userName)) {
                    ((TextView) findViewById(R.id.txtName1)).setText(users.get(i).getName());
                    ((TextView) findViewById(R.id.txtScore1)).setText(String.valueOf(users.get(i).getScore()));
                } else {
                    ((TextView) findViewById(R.id.txtName2)).setText(users.get(i).getName());
                    ((TextView) findViewById(R.id.txtScore2)).setText(String.valueOf(users.get(i).getScore()));
                }
            }
            getWordsSession();
        }
    }

    private void getWordsSession(){
        mDatabase.child("sessions").child(sessionId).child("words").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                words.clear();
                for (DataSnapshot word: dataSnapshot.getChildren()){
                    words.add(word.getValue().toString());
                }
                if (words.size()>0){
                    isFirstMove = false;
                }
                getLettersSession();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getLettersSession(){
        mDatabase.child("sessions").child(sessionId).child("letters").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                gridLetters.clear();
                for (DataSnapshot letter: dataSnapshot.getChildren()){
                    GridLetter gridLetter = letter.getValue(GridLetter.class);
                    int position = gridLetter.getRow()*15+ gridLetter.getCol();
                    scrabble.getChildAt(position).setSelected(true);
                    TextView txtLetter = scrabble.getChildAt(position)
                            .findViewById(R.id.gridLetter);
                    txtLetter.setText(gridLetter.getLetter());
                    gridLetters.add(gridLetter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void initUI(){
        initGrid();
        setLetters();

        findViewById(R.id.btnSwap).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                passes++;
                setLetters();
                nextMove();
            }
        });

        findViewById(R.id.btnPass).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                passes++;
                nextMove();
            }
        });

        findViewById(R.id.btnSubmit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getWord();
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void initGrid(){
        scrabble = findViewById(R.id.gridGame);
        for(int row = 0; row < 15; row++) {
            for(int col = 0; col < 15; col++) {
                final View newGrid;
                if (row==7&&col==7) {
                    newGrid = LayoutInflater.from(this).inflate(R.layout.grid_item_center, scrabble, false);
                } else {
                    newGrid = LayoutInflater.from(this).inflate(R.layout.grid_item, scrabble, false);
                }
                newGrid.setTag(R.string.row, row);
                newGrid.setTag(R.string.col, col);
                newGrid.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (selectedLetterId!=-1){
                            GridLetter gridLetter = new GridLetter();
                            gridLetter.setRow((int) v.getTag(R.string.row));
                            gridLetter.setCol((int) v.getTag(R.string.col));
                            gridLetter.setLetter(selectedLetter);
                            boolean canceled = false;
                            for (GridLetter gridLetter1: gridLetters){
                                if (gridLetter.getRow()==gridLetter1.getRow()&&gridLetter.getCol()==gridLetter1.getCol()){
                                    canceled = true;
                                    break;
                                }
                            }
                            for (GridLetter gridLetter1: gridLettersTemp){
                                if (gridLetter.getRow()==gridLetter1.getRow()&&gridLetter.getCol()==gridLetter1.getCol()){
                                    canceled = true;
                                    break;
                                }
                            }
                            if (!canceled) {
                                findViewById(R.id.btnSwap).setEnabled(false);
                                gridLettersTemp.add(gridLetter);
                                v.setSelected(true);
                                ((TextView)newGrid.findViewById(R.id.gridLetter)).setText(selectedLetter);
                                (findViewById(buttonsIds[selectedLetterId])).setEnabled(false);
                            }
                            (findViewById(buttonsIds[selectedLetterId])).setSelected(false);
                            selectedLetterId = -1;
                        }
                    }
                });
                scrabble.addView(newGrid);
            }
        }
    }

    private void setLetters(){
        if (selectedLetterId!=-1){
            (findViewById(buttonsIds[selectedLetterId])).setSelected(false);
        }

        char[] array = letters.toCharArray();
        Random random = new Random();

        if (selectedButtons.size()>0){
            for (int button: selectedButtons){
                if (lettersLeft>0) {
                    lettersLeft--;
                    int rInt = random.nextInt(array.length);
                    String letter = String.valueOf(array[rInt]);
                    ((Button) findViewById(button)).setText(letter);
                    (findViewById(button)).setEnabled(true);
                    (findViewById(button)).setSelected(false);
                    (findViewById(button)).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (v.isSelected()){
                                v.setSelected(false);
                                selectedButtons.remove((Integer) v.getId());
                                selectedLetterId = -1;
                            } else {
                                selectedLetter = ((Button) findViewById(buttonsIds[(int) v.getTag()])).getText().toString();
                                selectedLetterId = (int) v.getTag();
                                v.setSelected(true);
                                selectedButtons.add(v.getId());
                            }
                        }
                    });
                } else {
                    (findViewById(button)).setEnabled(true);
                    (findViewById(button)).setSelected(false);
                    Toast.makeText(this, "Больше нельзя заменить буквы", Toast.LENGTH_SHORT).show();
                }
            }
            selectedButtons.clear();
        } else {
            for (int i=0;i<7-selectedButtons.size();i++){
                if (lettersLeft>0) {
                    lettersLeft--;
                    int rInt = random.nextInt(array.length);
                    String letter = String.valueOf(array[rInt]);
                    ((Button) findViewById(buttonsIds[i])).setText(letter);
                    (findViewById(buttonsIds[i])).setTag(i);
                    (findViewById(buttonsIds[i])).setEnabled(true);
                    (findViewById(buttonsIds[i])).setSelected(false);
                    (findViewById(buttonsIds[i])).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (v.isSelected()){
                                v.setSelected(false);
                                selectedButtons.remove((Integer) v.getId());
                                selectedLetterId = -1;
                            } else {
                                selectedLetter = ((Button) findViewById(buttonsIds[(int) v.getTag()])).getText().toString();
                                selectedLetterId = (int) v.getTag();
                                v.setSelected(true);
                                selectedButtons.add(v.getId());
                            }
                        }
                    });
                } else {
                    Toast.makeText(this, "Больше нельзя заменить буквы", Toast.LENGTH_SHORT).show();
                }
            }
        }
        int buttonsLeft = 0;
        for (int button: buttonsIds){
            if (findViewById(button).isEnabled()){
                buttonsLeft++;
            }
        }
        if (buttonsLeft==0){
            mDatabase.child("sessions").child(sessionId).child("users")
                    .child(String.valueOf(users.get(0).getId())).child("passes").setValue(2);
            mDatabase.child("sessions").child(sessionId).child("users")
                    .child(String.valueOf(users.get(1).getId())).child("passes").setValue(2);
        }
    }

    private boolean isCenter(){
        boolean center = false;
        for (GridLetter gridLetter: gridLettersTemp){
            if (gridLetter.getRow() == 7 && gridLetter.getCol() == 7) {
                center = true;
                break;
            }
        }
        return center;
    }

    private void getWord(){
        if (gridLettersTemp!=null&& gridLettersTemp.size()>0) {
            if (isFirstMove) {
                if (isCenter()){
                    selectedLetterId = -1;
                    getWordString();
                } else {
                    cancelMove();
                    Toast.makeText(this, "Первое слово должно проходить через центральную клетку",
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                selectedLetterId = -1;
                getWordString();
            }
        }
    }

    private void getWordString(){
        int row1 = 0;
        int col1 = 0;
        int length1=0, length2=0;
        String word_last="";
        boolean isCanceled = false;

        ArrayList<String> newWords = new ArrayList<>();

        for (GridLetter gridLetter: gridLettersTemp){

            int row = gridLetter.getRow();
            StringBuilder word;
            for (int a = 0; a< gridLetter.getRow(); a++){
                int position = row*15+gridLetter.getCol()-a;
                TextView txtLetter = scrabble.getChildAt(position)
                        .findViewById(R.id.gridLetter);
                if (txtLetter.getText().toString().equals("")) {
                    col1 = (int) scrabble.getChildAt(position)
                            .getTag(R.string.col)+1;
                    break;
                }
            }
            word = new StringBuilder();
            for (int a = 0; a<15-col1; a++){
                int position = row*15+col1+a;
                TextView txtLetter = scrabble.getChildAt(position)
                        .findViewById(R.id.gridLetter);
                word.append(txtLetter.getText().toString());
                if (txtLetter.getText().toString().equals("")) {
                    length1 = word.length();
                    if (word.length()>1&&!word.toString().equals(word_last)){
                        word_last = word.toString();
                        if (!words.contains(word_last)) {
                            newWords.add(word_last);
                        } else {
                            isCanceled = true;
                            break;
                        }
                    }
                    break;
                }
            }

            word = new StringBuilder();

            int col = gridLetter.getCol();
            for (int a = 0; a< gridLetter.getRow(); a++){
                int position = row*15+col-15*a;
                TextView txtLetter = scrabble.getChildAt(position)
                        .findViewById(R.id.gridLetter);
                if (txtLetter.getText().toString().equals("")) {
                    row1 = (int) scrabble.getChildAt(position)
                            .getTag(R.string.row)+1;
                    break;
                }
            }
            for (int a=0;a<15-row1;a++){
                int position = row1*15+col+15*a;
                TextView txtLetter = scrabble.getChildAt(position)
                        .findViewById(R.id.gridLetter);
                word.append(txtLetter.getText().toString());
                if (txtLetter.getText().toString().equals("")) {
                    length2 = word.length();
                    if (word.length()>1&&!word.toString().equals(word_last)){
                        word_last = word.toString();
                        if (!words.contains(word_last)) {
                            newWords.add(word_last);
                        } else {
                            isCanceled = true;
                            break;
                        }
                    }
                    break;
                }
            }
        }

        if (isCanceled){
            cancelMove();
        } else {
            if (length1==1&&length2==1){
                cancelMove();
            } else {
                if (newWords.size()>0){
                    boolean canceled = false;
                    Set<String> words_final = new HashSet<>(newWords);
                    for (String word: words_final){
                        if (!dictionary.contains(word.toLowerCase())) {
                            canceled = true;
                            break;
                        }
                    }
                    if (!canceled) {
                        alertDialog.show();

                        boolean cross = false;

                        if (!isFirstMove) {
                            for (GridLetter gridLetter: gridLettersTemp){
                                for (GridLetter gridLetter1: gridLetters){
                                    if ((gridLetter.getRow() - 1 == gridLetter1.getRow()
                                            || gridLetter.getRow() + 1 == gridLetter1.getRow())
                                            &&(gridLetter.getCol() - 1 == gridLetter1.getCol()
                                            || gridLetter.getCol() + 1 == gridLetter1.getCol())) {
                                        cross = true;
                                        break;
                                    }
                                }
                            }
                        } else {
                            cross = true;
                        }

                        if (cross){
                            setLetters();
                            gridLetters.addAll(gridLettersTemp);

                            if (isFirstMove) isFirstMove = false;

                            int score = 0;
                            for (String word: newWords){
                                mDatabase.child("sessions").child(sessionId).child("words").push().setValue(word);
                                for (int i=0;i<word.length();i++){
                                    score += scores[letters.indexOf(word.charAt(i))];
                                }
                            }

                            for (GridLetter gridLetter: gridLettersTemp) {
                                mDatabase.child("sessions").child(sessionId).child("letters").push().setValue(gridLetter);
                            }
                            gridLettersTemp.clear();
                            mDatabase.child("sessions").child(sessionId).child("users").child(String.valueOf(userId))
                                    .child("score").setValue(currentUser.getScore()+score);
                            clearPasses();
                        } else {
                            alertDialog.dismiss();
                            cancelMove();
                        }

                    } else {
                        alertDialog.dismiss();
                        cancelMove();
                    }
                }
            }
        }

        //gridLetters.clear();
        //words.clear();
    }

    private void cancelMove(){
        for (int button: selectedButtons){
            (findViewById(button)).setEnabled(true);
        }
        for (GridLetter gridLetter: gridLettersTemp){
            scrabble.getChildAt(gridLetter.getRow()*15+ gridLetter.getCol()).setSelected(false);
            ((TextView)scrabble.getChildAt(gridLetter.getRow()*15+ gridLetter.getCol())
                    .findViewById(R.id.gridLetter)).setText("");
        }
        gridLetters.removeAll(gridLettersTemp);
        gridLettersTemp.clear();
        selectedButtons.clear();
        findViewById(R.id.btnSwap).setEnabled(true);
    }

    private void nextMove(){
        findViewById(R.id.btnSwap).setEnabled(true);
        mDatabase.child("sessions").child(sessionId).child("users")
                .child(String.valueOf(userId)).child("userMove").setValue(1);
        mDatabase.child("sessions").child(sessionId).child("users")
                .child(String.valueOf(userId)).child("passes").setValue(passes);
        if (users.indexOf(currentUser)==users.size()-1){
            mDatabase.child("sessions").child(sessionId).child("users")
                    .child(String.valueOf(users.get(0).getId())).child("userMove").setValue(0);
        } else {
            mDatabase.child("sessions").child(sessionId).child("users")
                    .child(String.valueOf(users.get(users.indexOf(currentUser)+1).getId())).child("userMove").setValue(0);
        }
    }

    private void clearPasses(){
        passes = 0;
        mDatabase.child("sessions").child(sessionId).child("users")
                .child(String.valueOf(users.get(0).getId())).child("passes").setValue(0);
        mDatabase.child("sessions").child(sessionId).child("users")
                .child(String.valueOf(users.get(1).getId())).child("passes").setValue(0);
        nextMove();
    }

    private void endGame(){
        //TODO
        findViewById(R.id.btnSubmit).setEnabled(false);
        findViewById(R.id.btnPass).setEnabled(false);
        findViewById(R.id.btnSwap).setEnabled(false);

        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
        builder.setMessage(R.string.game_over);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                exitSession();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                exitSession();
            }
        });
        try {
            builder.show();
        } catch (Exception e){
            e.printStackTrace();
        }
        gameOver = true;
    }

    private void exitSession(){
        mDatabase.child("sessions").child(sessionId).child("users").child(String.valueOf(userId)).removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mDatabase.child("sessions").child(sessionId).child("users").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.getChildrenCount()==0){
                                    mDatabase.child("sessions").child(sessionId).removeValue();
                                }
                                finish();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                });
    }

    @Override
    public void onBackPressed() {
        if (users.size()==2&&!alertDialog.isShowing()){
            AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
            builder.setMessage(R.string.session_exit);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    exitSession();
                }
            });
            builder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        exitSession();
        super.onDestroy();
    }
}