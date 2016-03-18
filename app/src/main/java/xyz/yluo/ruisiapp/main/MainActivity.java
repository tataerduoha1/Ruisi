package xyz.yluo.ruisiapp.main;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.Bind;
import butterknife.ButterKnife;
import cz.msebera.android.httpclient.Header;
import de.hdodenhof.circleimageview.CircleImageView;
import jp.wasabeef.recyclerview.animators.FadeInDownAnimator;
import xyz.yluo.ruisiapp.ConfigClass;
import xyz.yluo.ruisiapp.R;
import xyz.yluo.ruisiapp.TestActivity;
import xyz.yluo.ruisiapp.api.MainListArticleData;
import xyz.yluo.ruisiapp.api.MainListArticleDataHome;
import xyz.yluo.ruisiapp.article.ArticleNormalActivity;
import xyz.yluo.ruisiapp.http.AsyncHttpCilentUtil;
import xyz.yluo.ruisiapp.login.LoginActivity;
import xyz.yluo.ruisiapp.login.UserDakaActivity;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        RecyclerViewLoadMoreListener.OnLoadMoreListener {

    @Bind(R.id.toolbar)
    protected Toolbar toolbar;
    @Bind(R.id.fab1)
    protected FloatingActionButton fab1;
    @Bind(R.id.fab2)
    protected FloatingActionButton fab2;
    @Bind(R.id.fab)
    protected FloatingActionMenu fabMenu;
    @Bind(R.id.main_recycler_view)
    protected RecyclerView mRecyclerView;
    @Bind(R.id.main_refresh_layout)
    protected SwipeRefreshLayout refreshLayout;
    @Bind(R.id.drawer_layout)
    protected DrawerLayout drawer;
    @Bind(R.id.nav_view)
    protected NavigationView navigationView;
    @Bind(R.id.main_radiogroup)
    protected RadioGroup main_radiogroup;
    @Bind(R.id.radio01)
    protected RadioButton radio01;
    @Bind(R.id.radio02)
    protected RadioButton radio02;

    private CheckBox show_zhidin;


    //-1 为首页
    private static int CurrentFid =-1;
    private static String CurrentTitle = "首页";
    private int CurrentType = -1;
    //当前页数
    private int CurrentPage = 0;
    //在home界面时 0 代表第一页 1 代表板块列表
    private int HomeCurrentPage = 0;

    //一般板块/图片板块数据列表
    private List<MainListArticleData> mydatasetnormal = new ArrayList<>();
    private List<MainListArticleDataHome> mydatasethome = new ArrayList<>();

    private MainArticleListAdapter mRecyleAdapter;
    private MainHomeListAdapter mainHomeListAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    public static void open(Context context, int fid,String title){
        Intent intent = new Intent(context, MainActivity.class);
        CurrentFid = fid;
        CurrentTitle = title;
        System.out.print("\n>>>>>>>>>fid: "+CurrentFid+"title: "+CurrentTitle+"<<<<<<<<<<<\n");
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        //初始化
        init(CurrentFid, CurrentTitle);


        main_radiogroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radio01:
                        HomeCurrentPage = 0;
                        init(-1, "首页");
                        break;
                    case R.id.radio02:
                        HomeCurrentPage = 1;
                        init(-1, "首页");
                        break;
                }

            }
        });

        //TODO 根据当前板块加载内容
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                init(CurrentFid,CurrentTitle);
            }
        });

//        //按钮监听
        fabMenu.setOnMenuButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fabMenu.isOpened()) {
                    //Snackbar.make(v, fabMenu.getMenuButtonLabelText(), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                }
                fabMenu.toggle(true);
            }
        });

        fab1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "fab1", Toast.LENGTH_SHORT).show();
            }
        });
        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "fab2", Toast.LENGTH_SHORT).show();
            }
        });

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        View header = navigationView.getHeaderView(0);
        View nav_header_login = header.findViewById(R.id.nav_header_login);
        View nav_header_notlogin = header.findViewById(R.id.nav_header_notlogin);
        show_zhidin = (CheckBox) header.findViewById(R.id.show_zhidin);

        show_zhidin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    show_zhidin.setText("不显示置顶");
                    ConfigClass.CONFIG_ISSHOW_ZHIDIN = true;

                }else{
                    show_zhidin.setText("显示置顶帖");
                    ConfigClass.CONFIG_ISSHOW_ZHIDIN = false;
                }
                drawer.closeDrawer(GravityCompat.START);
                init(CurrentFid,CurrentTitle);
            }
        });

        //判断是否登陆
        if (ConfigClass.CONFIG_ISLOGIN) {
            nav_header_login.setVisibility(View.VISIBLE);
            nav_header_notlogin.setVisibility(View.GONE);
        } else {
            nav_header_notlogin.setVisibility(View.VISIBLE);
            nav_header_login.setVisibility(View.GONE);
        }

        CircleImageView userImge = (CircleImageView) header.findViewById(R.id.profile_image);
        userImge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ConfigClass.CONFIG_ISLOGIN) {
                    startActivity(new Intent(getApplicationContext(), UserDakaActivity.class));

                } else {
                    Intent i = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivityForResult(i, 1);
                }
            }
        });
    }

    //登陆页面返回结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            String result = data.getExtras().getString("result");//得到新Activity 关闭后返回的数据
            Toast.makeText(getApplicationContext(), "result" + result, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            init(-1, "首页");
        }
        else if (id == R.id.nav_01) {
            init(72,"西电睿思灌水专区");
        } else if (id == R.id.nav_02) {
            //106->110
            init(110,"校园交易");

        } else if (id == R.id.nav_03) {
            init(551,"西电问答");
        } else if (id == R.id.nav_04) {
            init(91,"考研交流");

        } else if (id == R.id.nav_05) {
            init(108,"我是女生");

        } else if (id == R.id.nav_06) {
            init(560,"技术博客");

        } else if (id == R.id.nav_07) {
            init(561,"摄影天地");
        }else if(id == R.id.nav_08){
            init(549, "文章天地");
        }else if(id == R.id.nav_test){
            startActivity(new Intent(this, TestActivity.class));
            // Handle the camera action
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //加载更多
    @Override
    public void onLoadMore() {
        //Toast.makeText(getApplicationContext(),"加载更多被触发",Toast.LENGTH_SHORT).show();

    }

    //一系列初始化
    private void init(int fid,String title) {

        //刷新
        refreshLayout.post(new Runnable() {
            @Override
            public void run() {
                refreshLayout.setRefreshing(true);
            }
        });

        //item 增加删除 改变动画
        mRecyclerView.setItemAnimator(new FadeInDownAnimator());
        mRecyclerView.getItemAnimator().setAddDuration(150);
        mRecyclerView.getItemAnimator().setRemoveDuration(10);
        mRecyclerView.getItemAnimator().setChangeDuration(10);

        if (fid == -1) {
            CurrentFid=-1;
            CurrentTitle = "首页";
            CurrentType = -1;
            main_radiogroup.setVisibility(View.VISIBLE);
        } else {
            CurrentType =0;
            CurrentFid = fid;
            CurrentTitle = title;
            main_radiogroup.setVisibility(View.GONE);

            //摄影板块
            if(CurrentFid==561){
                CurrentType =1;
            }
        }

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null){
            actionBar.setTitle(CurrentTitle);
        }

        //一般板块 和首页新帖
        if (CurrentType == 0 || (CurrentType == -1 && HomeCurrentPage == 0)) {
            //72灌水区
            //可以设置不同样式
            mLayoutManager = new LinearLayoutManager(this);
            //第二个参数是列数
            //mLayoutManager = new GridLayoutManager( getContext(),2);
            //加载更多实现
            mRecyclerView.addOnScrollListener(new RecyclerViewLoadMoreListener((LinearLayoutManager) mLayoutManager, this));
        } else if (CurrentType == 1 || (CurrentType == -1 && HomeCurrentPage == 1)) {
            //图片板 或者板块列表
            mLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        }

        mydatasetnormal.clear();
        mydatasethome.clear();

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyleAdapter = new MainArticleListAdapter(this, mydatasetnormal);
        mainHomeListAdapter = new MainHomeListAdapter(MainActivity.this,mydatasethome, 0);

        startGetData();

        mRecyclerView.setAdapter(mRecyleAdapter);

    }

    private void startGetData() {
        String url = "";
        if (CurrentType == -1) {
            url = "forum.php";
        } else {
            url = "forum.php?mod=forumdisplay&fid=";
            url = url + CurrentFid + "&page=" + CurrentPage;
        }

        AsyncHttpCilentUtil.get(getApplicationContext(), url, null, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                //普通板块
                if (CurrentType == 0) {
                    new GetNormalListTask(new String(responseBody)).execute();
                } else if (CurrentType == 1) {
                    //TODO
                    //图片板块
                    new GetImageListTask(new String(responseBody)).execute();
                } else if (CurrentType == -1 && HomeCurrentPage == 0) {
                    new GetHomeListTask_1(new String(responseBody)).execute();
                } else if (CurrentType == -1 && HomeCurrentPage == 1) {
                    new GetHomeListTask_2(new String(responseBody)).execute();
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(getApplicationContext(), "网络错误！！", Toast.LENGTH_SHORT).show();
                refreshLayout.setRefreshing(false);
            }
        });

    }

    //
    //获得一个普通板块文章列表数据 根据html获得数据
    public class GetNormalListTask extends AsyncTask<Void, Void, String> {

        private List<MainListArticleData> dataset = new ArrayList<>();
        private String res;

        public GetNormalListTask(String res) {
            this.res = res;
        }

        @Override
        protected String doInBackground(Void... params) {
            if(res!=""){
                Elements list = Jsoup.parse(res).select("div[id=threadlist]");
                Elements links = list.select("tbody");
                //System.out.print(links);
                MainListArticleData temp;
                for (Element src : links) {
                    if (src.getElementsByAttributeValue("class", "by").first() != null) {

                        String type = "normal";
                        //金币
                        if (src.select("th").select("strong").text() != "") {
                            type = "gold:" + src.select("th").select("strong").text().trim();
                        } else if (src.attr("id").contains("stickthread")) {
                            type = "zhidin";
                        } else {
                            type = "normal";
                        }
                        String title = src.select("th").select("a[href^=forum.php?mod=viewthread][class=s xst]").text();
                        String titleUrl = src.select("th").select("a[href^=forum.php?mod=viewthread][class=s xst]").attr("href");
                        //http://rs.xidian.edu.cn/forum.php?mod=viewthread&tid=836820&extra=page%3D1
                        String author = src.getElementsByAttributeValue("class", "by").first().select("a").text();
                        String authorUrl = src.getElementsByAttributeValue("class", "by").first().select("a").attr("href");
                        String time = src.getElementsByAttributeValue("class", "by").first().select("em").text().trim();
                        String viewcount = src.getElementsByAttributeValue("class", "num").select("em").text();
                        String replaycount = src.getElementsByAttributeValue("class", "num").select("a").text();

                        if(!ConfigClass.CONFIG_ISSHOW_ZHIDIN&&type.equals("zhidin")){
                            //do no thing
                        }else{
                            if (title != "" && author != "" && viewcount != "") {
                                //新建对象
                                temp = new MainListArticleData(title, titleUrl, type, author, authorUrl, time, viewcount, replaycount);
                                dataset.add(temp);
                            }
                        }

                    }
                }
            }
            return "";
        }

        @Override
        protected void onPostExecute(final String res) {

            mydatasetnormal.clear();
            mydatasetnormal.addAll(dataset);
            refreshLayout.setRefreshing(false);
            mRecyleAdapter.notifyItemRangeInserted(0, dataset.size());
        }
    }

    //
    //获得图片板块数据 图片链接、标题等  根据html获得数据
    public class GetImageListTask extends AsyncTask<Void, Void, String> {

        private String response;
        private List<MainListArticleData> imgdatas = new ArrayList<>();

        public GetImageListTask(String res) {
            this.response = res;
        }

        @Override
        protected String doInBackground(Void... params) {
            if (response != "") {

                Elements list = Jsoup.parse(response).select("ul[id=waterfall]");
                Elements imagelist = list.select("li");

                for (Element tmp : imagelist) {
                    //链接不带前缀
                    //http://rs.xidian.edu.cn/
                    String img = tmp.select("img").attr("src");
                    String url = tmp.select("h3.xw0").select("a[href^=forum.php]").attr("href");
                    String title = tmp.select("h3.xw0").select("a[href^=forum.php]").text();
                    String author = tmp.select("a[href^=home.php]").text();
                    String authorurl = tmp.select("a[href^=home.php]").attr("href");
                    String like = tmp.select("div.auth").select("a[href^=forum.php]").text();
                    //String title, String titleUrl, String image, String author, String authorUrl, String viewCount
                    MainListArticleData tem = new MainListArticleData(title, url, img, author, authorurl, like);
                    tem.setImageCard(true);
                    imgdatas.add(tem);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(final String res) {
            mydatasetnormal.clear();
            mydatasetnormal.addAll(imgdatas);
            refreshLayout.setRefreshing(false);
            mRecyleAdapter.notifyItemRangeInserted(0, imgdatas.size());
        }
    }


    //获取首页板块数据 最新帖子
    public class GetHomeListTask_1 extends AsyncTask<Void, Void, String> {

        private String response;
        private List<MainListArticleDataHome> simpledatas = new ArrayList<>();

        public GetHomeListTask_1(String res) {
            this.response = res;
        }

        @Override
        protected String doInBackground(Void... params) {
            if (response != "") {
                Elements list = Jsoup.parse(response).select("div[id=portal_block_314],div[id=portal_block_315]");
                Elements links = list.select("li");
                for (Element tmp : links) {

                    MainListArticleDataHome tempdata;
                    String titleurl = tmp.select("a[href^=forum.php]").attr("href").trim();
                    String title = tmp.select("a[href^=forum.php]").text();
                    //title="楼主：ansonzhang0123 回复数：0 总浏览数：0"
                    String message = tmp.select("a[href^=forum.php]").attr("title");
                    String User = message.split("\n")[0];
                    String ReplyCount = message.split("\n")[1];
                    String ViewCount = message.split("\n")[2];
                    //http://rs.xidian.edu.cn/home.php?mod=space&uid=124025
                    //String user = tmp.select("a[href^=]").text();
                    //String userurl = tmp.select("em").select("a").attr("href");

                    //去重
                    if (simpledatas.size() > 0) {
                        int i = 0;
                        for (i = 0; i < simpledatas.size(); i++) {
                            String have_url = simpledatas.get(i).getUrl();
                            if (have_url.equals(title)) {
                                break;
                            }
                        }
                        if (i == simpledatas.size()) {
                            //title titleurl User ReplyCount ViewCount
                            tempdata = new MainListArticleDataHome(title, titleurl, User, ReplyCount, ViewCount);
                            simpledatas.add(tempdata);
                        }
                    }
                    if (simpledatas.size() == 0) {
                        tempdata = new MainListArticleDataHome(title, titleurl, User, ReplyCount, ViewCount);
                        simpledatas.add(tempdata);


                    }
                }

            }
            return null;
        }

        @Override
        protected void onPostExecute(final String res) {
            mydatasethome.clear();
            mydatasethome.addAll(simpledatas);
            refreshLayout.setRefreshing(false);
            mainHomeListAdapter = new MainHomeListAdapter(MainActivity.this, mydatasethome, 0);
            mRecyclerView.setAdapter(mainHomeListAdapter);
            mainHomeListAdapter.notifyItemRangeInserted(0, mydatasethome.size());
        }
    }


    //获取首页板块数据 板块列表
    public class GetHomeListTask_2 extends AsyncTask<Void, Void, String> {
        private String response;
        private List<MainListArticleDataHome> simpledatas = new ArrayList<>();


        public GetHomeListTask_2(String res) {
            this.response = res;
        }


        @Override
        protected String doInBackground(Void... voids) {
            if (response != "") {
                Elements list = Jsoup.parse(response).select("#category_89,#category_101,#category_71,category_97,category_11").select("td.fl_g");
                for (Element tmp : list) {
                    MainListArticleDataHome datatmp;
                    String img = tmp.select("img[src^=./data/attachment]").attr("src").replace("./data", "data");
                    String url = tmp.select("a[href^=forum.php?mod=forumdisplay&fid]").attr("href");
                    String title = tmp.select("a[href^=forum.php?mod=forumdisplay&fid]").text();

                    String todaynew = tmp.select("em[title=今日]").text();
                    String actualnew = "";
                    if (todaynew != "") {
                        Pattern pattern = Pattern.compile("[0-9]+");
                        Matcher matcher = pattern.matcher(todaynew);
                        String tid = "";
                        while (matcher.find()) {
                            actualnew = todaynew.substring(matcher.start(), matcher.end());
                            //System.out.println("\ntid is------->>>>>>>>>>>>>>:" +  articleUrl.substring(matcher.start(),matcher.end()));
                        }
                    }
                    //String name, String image, String url, String todaypost
                    datatmp = new MainListArticleDataHome(title,img,url,actualnew);
                    simpledatas.add(datatmp);

                    //responseText.append("\nimg>>"+img+"\nurl>>"+url+"\ntitle>>"+title+"\ntoday>>"+todaynew+"\nactual>>"+actualnew);

                }

            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            mydatasethome.clear();
            mydatasethome.addAll(simpledatas);
            refreshLayout.setRefreshing(false);
            mainHomeListAdapter = new MainHomeListAdapter(MainActivity.this, mydatasethome, 1);
            mRecyclerView.setAdapter(mainHomeListAdapter);
            mainHomeListAdapter.notifyItemRangeInserted(0, mydatasethome.size());
        }

    }
}