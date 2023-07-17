package com.piddlepops.awardl;

import static com.google.android.material.transition.MaterialSharedAxis.X;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.piddlepops.awardl.databinding.ActivityMainBinding;
import com.piddlepops.awardl.databinding.GameOverDialogBinding;
import com.piddlepops.awardl.databinding.GameWinDialogBinding;
import com.piddlepops.awardl.databinding.HintDialogBinding;
import com.piddlepops.awardl.databinding.KeyboardBackButtonBinding;
import com.piddlepops.awardl.databinding.KeyboardEnterButtonBinding;
import com.piddlepops.awardl.databinding.KeyboardViewBinding;
import com.piddlepops.awardl.databinding.LetterViewBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.piddlepops.awardl.reswords.WordsResponse;

import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    int[][] letterGridArray = new int[6][5];

    char[] keyBoards = new char[]{'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', 'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'Z', 'X', 'C', 'V', 'B', 'N', 'M'};

    ArrayList<TextView> tvSpaces = new ArrayList<>();

    int activePosition = 0;
    int activeRow = 0;

    String correct = "";

    String name = "";
    String email;

    long seconds = 0;
    boolean shouldRunTimer = false;
    private TextView tvTimer;
    private WordsResponse wordsResponse;

    private Dialog dialog;

    private SharedPreferences sharedPreferences;


    public void runTimer() {

        long millis = seconds * 1000;
        String time = String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));

        if (seconds > (10 * 60)) {
            seconds = 0;
            shouldRunTimer = false;
            finish();
            startActivity(new Intent(MainActivity.this, SplashActivity.class));
        }
        if (shouldRunTimer) {
            tvTimer.setText(time);
            seconds++;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    runTimer();
                }
            }, 1000);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences("AWARDL", Context.MODE_PRIVATE);


        binding = ActivityMainBinding.inflate(getLayoutInflater());


        setContentView(binding.getRoot());

        if (getIntent().getStringExtra("name") != null && getIntent().getStringExtra("email") != null) {
            this.name = getIntent().getStringExtra("name");
            this.email = getIntent().getStringExtra("email");
        } else {
            finish();
        }

        HintDialogBinding hintDialogBinding = HintDialogBinding.inflate(getLayoutInflater());
        hintDialogBinding.cvCard.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_gradient));
        dialog = new Dialog(this);
        hintDialogBinding.getRoot().setOnClickListener(view -> dialog.dismiss());
        dialog.setContentView(hintDialogBinding.getRoot());

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            binding.getRoot().setRenderEffect(RenderEffect.createBlurEffect(
                    30f, //radius X
                    30f, //Radius Y
                    Shader.TileMode.MIRROR// X=CLAMP,DECAL,MIRROR,REPEAT
            ));
        }

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                shouldRunTimer = true;
                runTimer();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    binding.getRoot().setRenderEffect(null);
                }
            }
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initGame();
    }

    private void initGame() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.show();
        Retrofit retrofit = new Retrofit.Builder().baseUrl(sharedPreferences.getString("IP", "")).addConverterFactory(GsonConverterFactory.create(new Gson())).build();
        APIInterface api = retrofit.create(APIInterface.class);
        Call<WordsResponse> call = api.getWords();
        call.enqueue(new Callback<WordsResponse>() {
            @Override
            public void onResponse(Call<WordsResponse> call, Response<WordsResponse> response) {
                progressDialog.dismiss();
                if (response.body() != null && response.body().getData() != null && response.body().getData().size() > 0) {
                    wordsResponse = response.body();

                    //Show Dialog on Success Loading Words
                    dialog.show();
                    Window dWin = dialog.getWindow();
                    dWin.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                    dWin.setBackgroundDrawable(null);

                    //Init Actions
                    setWord();
                    initImage();
                    initGrid();
                    initBlankSpace();
                    initKeyboard();
                    initFooter();
                } else {
                    showNoWordsDialog();
                }
            }

            @Override
            public void onFailure(Call<WordsResponse> call, Throwable t) {
                progressDialog.dismiss();
                showNoWordsDialog();
            }
        });
    }

    private void showNoWordsDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Error while fetching words!");
        alertDialog.setPositiveButton("RESTART", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
                startActivity(new Intent(MainActivity.this, SplashActivity.class));
            }
        });
        alertDialog.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                System.exit(0);
            }
        });
        alertDialog.show();
    }

    private void initImage() {
        RelativeLayout relativeLayout = new RelativeLayout(this);
        ImageView imageView = new ImageView(this);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        imageView.setLayoutParams(new ViewGroup.LayoutParams((displayMetrics.widthPixels / 6) * 2, (displayMetrics.widthPixels / 6)));
        imageView.setImageResource(R.drawable.mptfawardl);
        relativeLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        relativeLayout.setGravity(RelativeLayout.CENTER_VERTICAL);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayout.addView(imageView);
        linearLayout.setGravity(Gravity.CENTER);
        relativeLayout.addView(linearLayout);
        TextView textView = new TextView(this);
        LinearLayout.LayoutParams timerLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textView.setLayoutParams(timerLp);
        timerLp.setMargins(15, 15, 0, 0);
        textView.setText("00:00");
        this.tvTimer = textView;
        Typeface timerTypeFace = getResources().getFont(R.font.gothambold);
        textView.setTypeface(timerTypeFace);
        textView.setTextColor(Color.BLACK);
        relativeLayout.addView(textView);

        View topMargin = new View(this);
        topMargin.setLayoutParams(new ViewGroup.LayoutParams(displayMetrics.widthPixels, ((displayMetrics.widthPixels / 10) / 4) / 2));
        binding.getRoot().addView(topMargin);
        binding.getRoot().addView(relativeLayout);
        View viewAfterImage = new View(this);
        viewAfterImage.setLayoutParams(new ViewGroup.LayoutParams(displayMetrics.widthPixels / 8, ((displayMetrics.widthPixels / 8) / 4) / 2));
        binding.getRoot().addView(viewAfterImage);
    }

    private void setWord() {
        this.correct = (wordsResponse.getData().toArray()[new Random().nextInt(wordsResponse.getData().size())] + "").toUpperCase(Locale.ROOT);
    }

    private void restartGame() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.show();
        Retrofit retrofit = new Retrofit.Builder().baseUrl(sharedPreferences.getString("IP", "")).addConverterFactory(GsonConverterFactory.create(new Gson())).build();
        APIInterface api = retrofit.create(APIInterface.class);
        Call<WordsResponse> call = api.getWords();
        call.enqueue(new Callback<WordsResponse>() {
            @Override
            public void onResponse(Call<WordsResponse> call, Response<WordsResponse> response) {
                progressDialog.dismiss();
                if (response.body() != null && response.body().getData() != null && response.body().getData().size() > 0) {
                    wordsResponse = response.body();
                    seconds = 0;
                    activePosition = 0;
                    shouldRunTimer = true;
                    activeRow = 0;
                    tvSpaces.clear();
                    binding.getRoot().removeAllViews();
                    binding.getRoot().invalidate();
                    runTimer();
                    initImage();
                    setWord();
                    initGrid();
                    initBlankSpace();
                    initKeyboard();
                    initFooter();
                } else {
                    showNoWordsDialog();
                }
            }

            @Override
            public void onFailure(Call<WordsResponse> call, Throwable t) {
                progressDialog.dismiss();
                showNoWordsDialog();
            }
        });
    }

    private void initBlankSpace() {
        View gap = new View(this);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        gap.setLayoutParams(new LinearLayout.LayoutParams(displayMetrics.widthPixels / 9, ((displayMetrics.widthPixels / 9))));
        binding.getRoot().addView(gap);
    }

    private void initFooter() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new ViewGroup.LayoutParams((int) (displayMetrics.widthPixels / 6.5), (int) (displayMetrics.widthPixels / 6.5)));
        imageView.setImageResource(R.drawable.custom_made_by_piddlepops);

        LinearLayout v = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        v.setGravity(Gravity.BOTTOM | Gravity.CENTER);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setLayoutParams(lp);
        v.addView(imageView);
        binding.getRoot().addView(v);
    }

    private void initGrid() {
        StringBuilder rows = new StringBuilder();
        LinearLayout rowsLinearLayout = new LinearLayout(this);
        rowsLinearLayout.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < letterGridArray.length; i++) {
            StringBuilder stringBuilder = new StringBuilder();
            LinearLayout columnLinearLayout = new LinearLayout(this);
            columnLinearLayout.setGravity(Gravity.CENTER);
            columnLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
            if (i == 0) {
                LinearLayout messageLayout = new LinearLayout(this);
                messageLayout.setGravity(Gravity.CENTER);
                messageLayout.setOrientation(LinearLayout.HORIZONTAL);
                TextView textView = new TextView(this);
                textView.setText(MessageFormat.format("GUESS THE {0}-LETTER WORD!", letterGridArray[0].length));
                textView.post(new Runnable() {
                    @Override
                    public void run() {
                        textView.setTextSize(18f);
                        textView.setTextColor(Color.BLACK);
                    }
                });
                Typeface tf = ResourcesCompat.getFont(this, R.font.gothambold);
                textView.setTypeface(tf);
                messageLayout.addView(textView);
                rowsLinearLayout.addView(messageLayout);
            }
            for (int j = 0; j < letterGridArray[i].length; j++) {
                stringBuilder.append(MessageFormat.format("{0}{1}{2}", i, j, j < (letterGridArray[i].length - 1) ? " " : ""));
                LetterViewBinding letterViewBinding = LetterViewBinding.inflate(getLayoutInflater());
                tvSpaces.add(letterViewBinding.tvLetter);
                columnLinearLayout.addView(letterViewBinding.getRoot());
            }
            if (i < letterGridArray.length - 1) {
                stringBuilder.append("\n");
            }
            rows.append(stringBuilder);
            rowsLinearLayout.addView(columnLinearLayout);
        }
        binding.getRoot().addView(rowsLinearLayout);
    }

    private void initKeyboard() {
        LinearLayout llBottom = new LinearLayout(this);
        llBottom.setGravity(Gravity.CENTER);
        LinearLayout llBottom2 = new LinearLayout(this);
        llBottom2.setGravity(Gravity.CENTER);
        LinearLayout llBottom3 = new LinearLayout(this);
        llBottom3.setGravity(Gravity.CENTER);

        for (int i = 0; i < keyBoards.length; i++) {
            View v;
            if (i < 10) {
                KeyboardViewBinding letterViewBinding = KeyboardViewBinding.inflate(getLayoutInflater());
                v = letterViewBinding.getRoot();
                v.setTag(keyBoards[i]);
                letterViewBinding.tvLetter.setText(String.valueOf(keyBoards[i]));
                llBottom.addView(letterViewBinding.getRoot());
                v.setOnClickListener(this::initOnClick);
            } else if (i < 19) {
                KeyboardViewBinding letterViewBinding = KeyboardViewBinding.inflate(getLayoutInflater());
                letterViewBinding.tvLetter.setText(String.valueOf(keyBoards[i]));
                v = letterViewBinding.getRoot();
                v.setTag(keyBoards[i]);
                llBottom2.addView(letterViewBinding.getRoot());
                v.setOnClickListener(this::initOnClick);
            } else {
                if (i == 19) {
                    KeyboardEnterButtonBinding keyboardEnterButtonBinding = KeyboardEnterButtonBinding.inflate(getLayoutInflater());
                    v = keyboardEnterButtonBinding.getRoot();
                    v.setTag("Enter");
                    llBottom3.addView(keyboardEnterButtonBinding.getRoot());
                    v.setOnClickListener(this::initOnClick);
                }
                KeyboardViewBinding letterViewBinding = KeyboardViewBinding.inflate(getLayoutInflater());
                v = letterViewBinding.getRoot();
                v.setTag(keyBoards[i]);
                letterViewBinding.tvLetter.setText(String.valueOf(keyBoards[i]));
                llBottom3.addView(letterViewBinding.getRoot());
                v.setOnClickListener(this::initOnClick);
                if (i == keyBoards.length - 1) {
                    KeyboardBackButtonBinding keyboardBackButtonBinding = KeyboardBackButtonBinding.inflate(getLayoutInflater());
                    v = keyboardBackButtonBinding.getRoot();
                    v.setTag("Back");
                    llBottom3.addView(keyboardBackButtonBinding.getRoot());
                    v.setOnClickListener(this::initOnClick);
                }
            }
        }

        binding.getRoot().addView(llBottom);
        binding.getRoot().addView(llBottom2);
        binding.getRoot().addView(llBottom3);
    }

    public void scaleView(View v, float startScale, float endScale) {
        Animation anim = new ScaleAnimation(
                1f, 1f, // Start and end values for the X axis scaling
                startScale, endScale, // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0f, // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 1f); // Pivot point of Y scaling
        anim.setFillAfter(true); // Needed to keep the result of the animation
        anim.setDuration(300);
        v.startAnimation(anim);
    }

    public void scaleDownView(View v, float startScale, float endScale) {
        Animation anim = new ScaleAnimation(
                1f, 1f, // Start and end values for the X axis scaling
                startScale, endScale, // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 1f, // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 0f); // Pivot point of Y scaling
        anim.setFillAfter(true); // Needed to keep the result of the animation
        anim.setDuration(300);
        v.startAnimation(anim);
    }

    private void initOnClick(View v) {
        if (v.getTag().toString().equals("Enter")) {
            if (activePosition < (activeRow + 1) * letterGridArray[0].length) {
                return;
            }
            if (activePosition % letterGridArray[0].length == 0) {
                boolean checkIfCorrect = true;
                int counter = 0;
                if (correct.toCharArray().length != letterGridArray[0].length) {
                    return;
                }
                int changeSize = 0;


                for (int i = activeRow * letterGridArray[0].length; i < (activeRow + 1) * letterGridArray[0].length; i++) {
                    TextView viewToAnimate = tvSpaces.get(i);

                    if (!(correct.toCharArray()[counter] == viewToAnimate.getText().toString().toCharArray()[0])) {
                        checkIfCorrect = false;
                    }

                    if (correct.toCharArray()[counter] == viewToAnimate.getText().toString().toCharArray()[0]) {
                        LinearLayout linearLayout = (LinearLayout) viewToAnimate.getParent();
                        linearLayout.setBackgroundColor(getColor(R.color.green));
                    } else {
                        LinearLayout linearLayout = (LinearLayout) viewToAnimate.getParent();
                        linearLayout.setBackgroundColor(getColor(R.color.darkend_Yellow));

                        char activeChar = viewToAnimate.getText().toString().toCharArray()[0];

                        ArrayList<Integer> activeRepeatCountPositions = new ArrayList<>();

                        int correctCharCount = 0;

                        ArrayList<Integer> correctCharsPos = new ArrayList<>();

                        for (int j = activeRow * letterGridArray[0].length; j < (activeRow + 1) * letterGridArray[0].length; j++) {
                            if (tvSpaces.get(j).getText().charAt(0) == activeChar) {
                                activeRepeatCountPositions.add(j);
                            }
                        }

                        for (int ci = 0; ci < correct.toCharArray().length; ci++) {
                            if (correct.toCharArray()[ci] == activeChar) {
                                correctCharCount++;
                                correctCharsPos.add((activeRow * letterGridArray[0].length) + ci);
                            }
                        }

                        for (int correctPos : correctCharsPos) {
                            activeRepeatCountPositions.remove((Object) correctPos);
                        }

                        int filledCount = 0;

                        for (int cpi = 0; cpi < correctCharsPos.size(); cpi++) {
                            if (activeChar == tvSpaces.get(correctCharsPos.get(cpi)).getText().charAt(0)) {
                                filledCount++;
                            }
                        }

                        if (filledCount == correctCharsPos.size()) {
                            linearLayout.setBackgroundColor(getColor(R.color.gray));
                        }

                        int tobeReplacedCount = correctCharCount - filledCount;

                        for (int ii = 0; ii < activeRepeatCountPositions.size(); ii++) {
                            if (ii < tobeReplacedCount) {
                                ((LinearLayout) tvSpaces.get(activeRepeatCountPositions.get(ii)).getParent()).setBackgroundColor(getColor(R.color.darkend_Yellow));
                            } else {
                                ((LinearLayout) tvSpaces.get(activeRepeatCountPositions.get(ii)).getParent()).setBackgroundColor(getColor(R.color.gray));
                            }
                        }
                    }

                    counter++;
                    new Handler().postDelayed(() -> {
                        viewToAnimate.animate().withLayer()
                                .rotationY(90)
                                .setDuration(300)
                                .withEndAction(
                                        () -> {
                                            // second quarter turn
                                            viewToAnimate.setRotationY(-90);
                                            viewToAnimate.animate().withLayer()
                                                    .rotationY(0)
                                                    .setDuration(300)
                                                    .start();
                                        }
                                ).start();
                    }, 300L * counter);
                }

                activeRow++;

                if (!checkIfCorrect && activeRow == letterGridArray.length) {
                    GameOverDialogBinding gameOverDialogBinding = GameOverDialogBinding.inflate(getLayoutInflater());
                    gameOverDialogBinding.cvCard.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_gradient));
                    gameOverDialogBinding.tvSorryMsg.setText(MessageFormat.format("Sorry {0},\nthe word was:", name));
                    gameOverDialogBinding.tvCorrectWord.setText(correct);
                    gameOverDialogBinding.tvScore.setText(MessageFormat.format("Score: {0}", "10"));
                    postResult(10, activeRow);
                    Dialog dialog1 = new Dialog(this);
                    gameOverDialogBinding.btnBackToMain.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog1.dismiss();
                            finish();
                            startActivity(new Intent(MainActivity.this, SplashActivity.class));
                        }
                    });

                    gameOverDialogBinding.btnNew.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog1.dismiss();
                            restartGame();
                        }
                    });
                    //gameOverDialogBinding.getRoot().setOnClickListener(view -> dialog1.dismiss());
                    dialog1.setContentView(gameOverDialogBinding.getRoot());
                    dialog1.setCancelable(false);
                    dialog1.show();
                    shouldRunTimer = false;
                    Window dWin = dialog1.getWindow();
                    dWin.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                    dWin.setBackgroundDrawable(null);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        binding.getRoot().setRenderEffect(RenderEffect.createBlurEffect(
                                30f, //radius X
                                30f, //Radius Y
                                Shader.TileMode.MIRROR// X=CLAMP,DECAL,MIRROR,REPEAT
                        ));
                    }

                    dialog1.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                binding.getRoot().setRenderEffect(null);
                            }
                        }
                    });
                } else if (checkIfCorrect) {
                    GameWinDialogBinding gameWinDialogBinding = GameWinDialogBinding.inflate(getLayoutInflater());
                    gameWinDialogBinding.cvCard.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_gradient));
                    gameWinDialogBinding.tvWinMsg.setText(MessageFormat.format("CONGRATULATIONS\n{0}!", name));
                    float point = 0.0f;
                    long time_diff = 0;
                    if (seconds < 600) {
                        time_diff = 600 - seconds;
                    }
                    switch (activeRow) {
                        case 1:
                            point = time_diff + 300;
                            break;
                        case 2:
                            point = time_diff + 200;
                            break;
                        case 3:
                            point = time_diff + 132;
                            break;
                        case 4:
                            point = time_diff + 100;
                            break;
                        case 5:
                            point = time_diff + 80;
                            break;
                        case 6:
                            point = time_diff + 66;
                            break;
                    }

                    gameWinDialogBinding.tvScore.setText(MessageFormat.format("Score: {0}", point));
                    postResult(point, activeRow);
                    Dialog dialog1 = new Dialog(this);
                    gameWinDialogBinding.btnBackToMain.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog1.dismiss();
                            finish();
                            startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        }
                    });
                    gameWinDialogBinding.btnNew.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog1.dismiss();
                            restartGame();
                        }
                    });
                    //gameWinDialogBinding.getRoot().setOnClickListener(view -> dialog1.dismiss());
                    dialog1.setContentView(gameWinDialogBinding.getRoot());
                    dialog1.setCancelable(false);
                    dialog1.show();
                    shouldRunTimer = false;
                    Window dWin = dialog1.getWindow();
                    dWin.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                    dWin.setBackgroundDrawable(null);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        binding.getRoot().setRenderEffect(RenderEffect.createBlurEffect(
                                30f, //radius X
                                30f, //Radius Y
                                Shader.TileMode.MIRROR// X=CLAMP,DECAL,MIRROR,REPEAT
                        ));
                    }

                    dialog1.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                binding.getRoot().setRenderEffect(null);
                            }
                        }
                    });
                }
            }
        } else if (v.getTag().toString().equals("Back")) {
            if (activePosition > 0 && activePosition > (activeRow * letterGridArray[0].length)) {
                activePosition--;
                tvSpaces.get(activePosition).setText("");
                scaleDownView(tvSpaces.get(activePosition), 1f, 0f);
            }
        } else if (activePosition < tvSpaces.size() && activePosition < ((activeRow + 1) * letterGridArray[0].length)) {
            tvSpaces.get(activePosition).setText("" + v.getTag());
            scaleView(tvSpaces.get(activePosition), 0f, 1f);
            activePosition++;
        }
    }

    private void postResult(float point, int noOfGuesses) {
        Retrofit retrofit = new Retrofit.Builder().baseUrl(sharedPreferences.getString("IP", "")).addConverterFactory(GsonConverterFactory.create(new Gson())).build();
        APIInterface api = retrofit.create(APIInterface.class);
        PostResultModel postBody = new PostResultModel(name, email, seconds, noOfGuesses, point);
        api.postResult(postBody).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    response.body().string();
                } catch (Exception e) {
                    //Snackbar.make(binding.getRoot(), "" + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                //Snackbar.make(binding.getRoot(), "" + t.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }
}
