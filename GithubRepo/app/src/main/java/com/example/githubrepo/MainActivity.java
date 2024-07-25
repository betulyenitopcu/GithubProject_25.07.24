package com.example.githubrepo;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private EditText usernameInput;
    private Button searchButton;
    private TextView userName;
    private ImageView avatar;
    private RecyclerView repoList;
    private TextView followerCount;
    private TextView followingCount;

    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usernameInput = findViewById(R.id.usernameInput);
        searchButton = findViewById(R.id.searchButton);
        userName = findViewById(R.id.userName);
        avatar = findViewById(R.id.avatar);
        repoList = findViewById(R.id.repoList);
        followerCount = findViewById(R.id.followerCount);
        followingCount = findViewById(R.id.followingCount);

        repoList.setLayoutManager(new LinearLayoutManager(this));

        client = new OkHttpClient();

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameInput.getText().toString();
                if (!TextUtils.isEmpty(username)) {
                    new FetchGitHubProfile().execute(username);
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a username", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private class FetchGitHubProfile extends AsyncTask<String, Void, String[]> {
        @Override
        protected String[] doInBackground(String... params) {
            String username = params[0];
            String[] result = new String[2];
            try {
                // Fetch user profile
                Request userRequest = new Request.Builder()
                        .url("https://api.github.com/users/" + username)
                        .addHeader("Authorization", "token " + BuildConfig.GITHUB_TOKEN)
                        .build();
                Response userResponse = client.newCall(userRequest).execute();
                if (userResponse.isSuccessful()) {
                    String userJsonString = userResponse.body().string();
                    JSONObject userJson = new JSONObject(userJsonString);
                    String name = userJson.getString("name");
                    String avatarUrl = userJson.getString("avatar_url");
                    String followers = userJson.getString("followers");
                    String following = userJson.getString("following");
                    result[0] = name + "|" + avatarUrl + "|" + followers + "|" + following;
                }

                // Fetch repositories
                Request reposRequest = new Request.Builder()
                        .url("https://api.github.com/users/" + username + "/repos")
                        .addHeader("Authorization", "token " + BuildConfig.GITHUB_TOKEN)
                        .build();
                Response reposResponse = client.newCall(reposRequest).execute();
                if (reposResponse.isSuccessful()) {
                    String reposJsonString = reposResponse.body().string();
                    JSONArray reposArray = new JSONArray(reposJsonString);
                    List<String> repoNames = new ArrayList<>();
                    List<String> repoDescriptions = new ArrayList<>();
                    List<String> repoStars = new ArrayList<>();
                    for (int i = 0; i < reposArray.length(); i++) {
                        JSONObject repoJson = reposArray.getJSONObject(i);
                        repoNames.add(repoJson.getString("name"));
                        repoDescriptions.add(repoJson.optString("description", "No description"));
                        repoStars.add(repoJson.getString("stargazers_count"));
                    }
                    result[1] = repoNames.toString() + "|" + repoDescriptions.toString() + "|" + repoStars.toString();
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result[0] != null) {
                String[] userInfo = result[0].split("\\|");
                userName.setText(userInfo[0]);
                Glide.with(MainActivity.this).load(userInfo[1]).into(avatar);
                followerCount.setText("Followers: " + userInfo[2]);
                followingCount.setText("Following: " + userInfo[3]);
                userName.setVisibility(View.VISIBLE);
                avatar.setVisibility(View.VISIBLE);
                followerCount.setVisibility(View.VISIBLE);
                followingCount.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(MainActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }

            if (result[1] != null) {
                String[] repoInfo = result[1].split("\\|");
                List<GitHubRepo> repos = new ArrayList<>();
                String[] names = repoInfo[0].replace("[", "").replace("]", "").split(", ");
                String[] descriptions = repoInfo[1].replace("[", "").replace("]", "").split(", ");
                String[] stars = repoInfo[2].replace("[", "").replace("]", "").split(", ");
                for (int i = 0; i < names.length; i++) {
                    GitHubRepo repo = new GitHubRepo();
                    repo.setName(names[i]);
                    repo.setDescription(descriptions[i]);
                    repo.setStars(stars[i]);
                    repos.add(repo);
                }
                RepoAdapter adapter = new RepoAdapter(MainActivity.this, repos);
                repoList.setAdapter(adapter);
            } else {
                Toast.makeText(MainActivity.this, "Failed to load repositories", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
