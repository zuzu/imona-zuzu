import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.StringItem;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.RecordStore;

import com.j_phone.io.BrowserConnection;
import com.j_phone.system.DeviceControl;
import com.jblend.io.InflateInputStream;
import com.jblend.micro.lcdui.LocalizedTextBox;
import com.jblend.micro.lcdui.LocalizedTextField;

/** iMona@zuzu
 * @author zuzu
 * @version  1.0.3
 */

public final class iMona extends MIDlet {
	/**　iMonaの画面　*/
	mainCanvas canv;
	/** アプリの起動 */
	public final void startApp() {  //ここから始まる
		if(canv == null){	//起動時
			canv = new mainCanvas(this);
			canv.disp = Display.getDisplay(this);
			canv.disp.setCurrent(canv);  //表示させるクラスを指定する
		} else {	//再開
/*
			if((stat & 0x0000400) == 0){	//設定中ではない場合
				canv.commandAction(command[2], null);	//取り消し
			}
*/
			canv.stat |= 0x1000000;	//画面更新
		}

	}
	/**　アプリの一時停止 */
	public final void pauseApp() {
		if(canv != null)
			canv.stat |= 0x1000000;	//画面更新
	}
	/** アプリの終了 */
	public final void destroyApp(boolean unconditional) {
		canv.saveSetting();
		canv.thread = null;
	}
} //classiMonaの終わり

/**
iMonaの全ての処理が格納されているクラス
@author 作者 & zuzu
@see iMona
*/
final class mainCanvas extends Canvas implements Runnable,CommandListener {
	/**	文字入力時に使用するform */
	Form inputForm;
	/**	バージョン用変数 */
	String version = "1.0.3Xmas";
	/**	色々なところで使うテキストフィールド。日本語入力向けのLocalizedTextFieldを使用しています。 */
	LocalizedTextField /*bname,*/ btitle, bres, bboard, bthread, tfield, tfield2;
	/**	色々なところで使うテキストボックス。日本語入力向けのLocalizedTextBoxを使用しています。 */
	LocalizedTextBox tbox;
	/** 色々なところで繰り返し使うChoiceGroup */
	ChoiceGroup choice;
	/** BBSディレクトリ名 */
	String bbsname;

	/** クッションリンクのリストを表示するためのString変数 */
	String cushionlinklist = "zuzu鯖\nimona.zuzu-service.net/ime.php?\n\n桜楓椿鯖\nime.k2y.info/?";
	//String searchurl = "";
	/** 書き込み時に入力した内容を保存するためのString変数 */
	String name = "", mail = "", bodytext = "", title = "";
	
	/** cookieを収納するString変数 **/
	String cookie = "";

	/**　現在使用中の中間サーバーを保存するためのString変数　*/
	String server = "";
	/** 拡張オプション */
	String extendedoption = "";
	/** クッションリンク */
	String cushionlink = "";
	//String resdata;
	byte resdata[];
	/** 検索文字列 */
	String searchtext = "";
	//
	/** forなどの繰り返しを高速化するための一時変数。今は滅多に使わない */
	int i_length; //forなどの繰り返し用の一時変数

	/**　バグ報告の為に追加した変数。　*/
	StringBuffer bagdata = new StringBuffer("");
	//過去の名前入力履歴
	//String Namelist[] = new String[10];
	//int Namenum = 0;
	/** NGワード判定用String変数	 */
	String ngword;
	//String textdata[] = new String[20];
	//cushionlinklist
	//name
	//mail
	//bodytext
	//title
	//server
	//廃止：リンクは必ず行の先頭にある。
	//同一スレッド内,他のスレッド,2ch外

	/** *詳細不明<br>
	 * XXXYYYYZZZ<br>
     * XXX:リンクの文字数(byte数)(Linklist[x] / 10000000)<br>
	 * YYYY:リンクのある行((Linklist[x] / 1000) % 10000)<br>
     * ZZZ:リンクのある桁(byte数)(Linklist[x] % 1000)　*/
	int Linklist[] = new int[40];

	/** 詳細不明<br>　ZZZ:リンクのある桁(文字数)　*/
	int Linklist2[] = new int[40];
	/**  詳細不明<br>リンクのURLを収納する配列 */
	String Linkurllist[] = new String[40];

	/**　詳細不明<br>リンク元を格納するところ。中身はBookMarkDataと同じ方式。<br>
	 * n:整数<br>
	　*　Linkref[3n+0]:板番号<br>
	　*　Linkref[3n+1]:スレッド番号<br>
	　*　Linkref[3n+2]:レス番号<br>
	　*　Linkref[0],[1],[2]は読み込もうとしている板番号、スレッド番号、レス番号を表す<br>
	 * 　*/
	int Linkref[] = new int[50];	//リンク元を格納するところ。中身はBookMarkDataと同じ方式。
	//n:整数
	//Linkref[3n+0]:板番号
	//Linkref[3n+1]:スレッド番号
	//Linkref[3n+2]:レス番号
	//Linkref[0],[1],[2]は読み込もうとしている板番号、スレッド番号、レス番号を表す
	/** 詳細不明<br> リンク元を格納するところ。中身はBookMarkDataと同じ方式。*/
	int  Linkrefsc[] = new int[20];	//リンク元を格納するところ。中身はBookMarkDataと同じ方式。
	//List alist;
	/** 通信用のoutput */
	ByteArrayOutputStream outarray;
	/** ダウンロードしたデータを格納する場所 */
	byte dlarray[];	//ダウンロードしたデータを格納する場所
	/** 板データを格納する場所 */
	byte brdarray[];	//板データを格納する場所
	/**　親クラス　　*/
	iMona parent;

	/**　MIDPで必須のDisplayクラス。ここに全てを描画する。　*/
	Display disp;

	/** リストボックス。いわゆる各種メニュー表示用　　*/
	String ListBox[];
	//-/String DataFolder[];
	/** 画面の幅 */
	int width;
	/**　画面の高さ */
	int height;
//	int nThread[];		//スレッドの番号を格納する配列
//	String ThreadName[];//スレッド名を格納する配列
//	int nRes[];			//スレッドのレス数を格納する配列
	/**　詳細不明。どうやらDivStr用の一時変数っぽい　
	 * @see MainCanvas#DivStr*/
	int iDivStr[];// = new String[100];
	/**　現在表示中のレスの中身。ただ改行がないため見にくい。　　*/
	String resstr;
	/** 詳細不明。レスを格納する配列かと思われる。 */
	String DivStr[];// = new String[100];
//	byte Res[][];//レスを格納する配列
	//String ResElements[];//レスを格納する配列
	/**　ﾌﾞｯｸﾏｰｸのタイトル文字列　*/
	String BookMark[] = new String[200];
	/**　ﾌﾞｯｸﾏｰｸのボード番号+スレッド番号+ﾚｽ(ｽﾚ)番号[ｻｲｽﾞBookMark.length*3] AAAABBBCCCC　*/
	int BookMarkData[];
	/**　トリミング済みのブックマーク名　*/
	String tBookMark[] = new String[200];
	/**　ブックマークの接続URL　*/
	String BookMarkUrl[] = new String[200];

	//キャッシュ関係
	/** 現在読んでいるキャッシュのインデックス。キャッシュ系の配列はこれをキーにして読み込む */
	int nCacheIndex;
	/** 板名や、スレッドタイトル */
	String CacheTitle[] = new String[40];
	/** スレッドのデータ(板のキャッシュの時はnull) */
	byte CacheResData[][][] = new byte[40][][];
	/** 板のデータ(スレッドタイトル)(スレッドのキャッシュの時はnull) */
	String CacheBrdData[][] = new String[40][];
	/** 板のデータ(レス数)(スレッドのキャッシュの時はnull) */
	int nCacheBrdData[][] = new int[40][];
	/** スレッド番号(スレッドのキャッシュの時はnCacheTh[x][0]を使う) */
	int nCacheTh[][] = new int[40][];
	/** 板番号 */
	int nCacheBrd[] = new int[40];
	/** start */
	int nCacheSt[] = new int[40];
	/** to */
	int nCacheTo[] = new int[40];
	/**　all */
	int nCacheAll[] = new int[40];
	/**　status　*/
	int nCacheStat[] = new int[40];
	/**　time to live。古いキャッシュほど値が大きくなる。(-1:このキャッシュは無効or空)　*/
	int nCacheTTL[] = new int[40];
	/**　キャッシュの存在チェック用　*/
	int nCacheInfo[];
	/**
	 * 配色設定用。でも容量が増えるのでまだ微妙。
	 */
	int ColPreset[][] = {//color scheme
	//    背景色       文字色       薄い色       中間の濃さ   ﾘｽﾄ選択色    ﾘｽﾄ選択色(F) 名前(ﾚｽ)     注意色       ﾘﾝｸ色(選択)  ﾘﾝｸ色(未選択) ﾘﾝｸ色(選択,ｷｬｯｼｭ) ﾘﾝｸ色(ｷｬｯｼｭ)
	//    0   1   2    3   4   5    6   7   8    9   10  11   12  13  14   15  16  17   18  19  20   21  22  23   24  25  26   27  28  29    30  31  32        33  34  35
		{ 239,239,239,   0,  0,  0, 192,192,192, 128,128,128, 192,192,255, 255,165,  0,   0,128,  0, 204, 51,  0, 192,192,255, 255,192,192,  128,128,255,      255,128,128},	//通常(2ch風)
		{ 255,255,255,   0,  0,  0, 192,192,192, 128,128,128, 128,128,255, 255,128,128,   0,128,  0, 204, 51,  0, 128,128,255, 255,128,128,   64, 64,255,      255, 64, 64},	//ハイコントラスト
		{   0,  0,  0, 225,225,225, 148,148,148, 128,128,128, 140,140,140, 128,128,128, 192,192,192, 204, 51,  0,  98, 98, 98,   0,  0,  0,  128,128,128,       64, 64, 64},	//黒地灰字
		{ 255,251,244, 126, 94, 87, 175,142,149, 128,128,128, 249,163,150, 255,200,140, 195,146, 82, 195,146, 82, 250,173,160, 255,230,180,  255,148,148,      255,204,136},	//暖色系
	};
	/**
	 * 現在の配色設定
	 */
	int[] ColScm;	//配色設定
	/**
	 * サーバーURL用の配列
	 */
	String server_url[] = { //鯖URL配列
			"http://imona.coresv.com/",
			"http://imona.net/",
			"http://imona.zuzu-service.net/o/"
	};// = new String[100];
	/**
	 * data,SavNoInfo<br>
	 * o0 - スレ一覧で一度に読み込む数<br>
	 * o1 - 一度に読み込むレスの数<br>
	 * _2 - x現在見ているスレの番号(絶対値)<br>
	 * _3 - x現在見ている板番号(絶対値)<br>
	 * _4 - x現在見ているスレの番号(現在読み込んでいる中のはじめからのインデックス(0-data[0]))<br>
	 * _5 - x現在見ているスレの番号のインデックス(0-400～500)<br>
	 * _6 - 現在見ているレス番号(data[7]からの相対値、2chでのレス番号(絶対値)=data[6]+data[7])<br>
	 * _7 - x現在見ているスレッドのレスの始まり<br>
	 * _8 - x現在見ているスレッドのレスの終わり<br>
	 * _9 - x現在見ているスレッドの全レス数<br>
	 * _10 -  リスト内文字列の移動分<br>
	 * _11 -  選択の移動分<br>
	 * _12 -  LISTBOX Y座標<br>
	 * _13 -  LISTBOX 縦幅<br>
	 * _14 -  DLするサイズ<br>
	 * *15 -  展開後のサイズ<br>
	 * _16 -  現在DLしている展開後のサイズ(進行形)<br>
	 * _17 -  gzip圧縮後のサイズ<br>
	 * _18 -  圧縮前のサイズ<br>
	 * _19 -  設定(詳細)の選択位置(移動分) リストボックスのリストの数<br>
	 * _20+21 -  設定(メインメニュー)の選択位置<br>
	 * _22+23 -  カテゴリリストの選択位置<br>
	 * _24+25 -  板リストの選択位置<br>
	 * _26 -  設定 / 選択出来る数字の上限<br>
	 * _27 -  設定 / 選択している桁<br>
	 * _28 -  設定 / 選択している値<br>
	 * _29 -  設定 / 選択出来る数字の上限の桁数-1<br>
	 * _30 -  文字の高さ<br>
	 * _31 -  文字の描画位置(ベースラインの位置)font.getAscent<br>
	 * _32 -  ベースラインから下 font.getDescent<br>
	 * _33 -  文字の幅(文字の高さ/2[+1])<br>
	 * o34 -  行間<br>
	 * o35 -  １スクロールで移動する量<br>
	 * o36 -  文字のサイズ(0:小 1:中 2:大)<br>
	 * _37 -  壁紙x座標<br>
	 * _38 -  壁紙y座標<br>
	 * _39 -  壁紙のサイズ<br>
	 * o40 -  ﾌﾞｯｸﾏｰｸの数<br>
	 * .41 -  壁紙の位置<br>
	 * _42 -  SeparateStringで区切るときの最大横幅のバイト数<br>
	 * _43 -  直前に選択したﾌﾞｯｸﾏｰｸのインデックス<br>
	 * _44 -  板リストをどこまで読み込んだか<br>
	 * _45 -  レスをどこまで処理したか<br>
	 * _46 -  パケット代<br>
	 * o47 -  パケット代(合計)<br>
	 * _48 -  横スクロール分(AA MODE用)<br>
	 * o49 -  保存する設定(byte 0xFFまで)<br>
	 * 　　0x01 - 　レススクロール時にボタンを押しっぱなしでスクロールする<br>
	 * 　　0x02 - 　右上に秒の表示<br>
	 * 　　0x04 - 　IDの表示<br>
	 * 　　0x08 - 　時刻の表示<br>
	 * 　　0x10 - 　ﾒｱﾄﾞの表示<br>
	 * 　　0x20 - 　SO用ｽｸﾛｰﾙ処理<br>
	 * 　　0x40 - 　名前の表示<br>
	 * 　　0x80 - 　keyrepeat xxx最新ﾚｽで1も読む<br>
	 * _50 -  文字列表示のx座標<br>
	 * _51 -  文字列表示のy座標<br>
	 * _52 -  Box文字列表示のx座標<br>
	 * _53 -  Box文字列表示のy座標<br>
	 * _54 -  ページスクロールの行数<br>
	 * o55 -  最新ﾚｽで読む数<br>
	 * o56 -  ｽﾚ情報の表示(2:する 1:しない) ｽﾚ一覧のﾒﾆｭｰ数<br>
	 * o57 -  保存するフラグ2<br>
	 * 　　0x00000001 - 　常にON<br>
	 * 　　0x00000002 - 　AAの表示<br>
	 * 　　0x00000004 - 　URLの表示<br>
	 * 　　0x00000008 - 　右上に時刻の表示(時：分)<br>
	 * 　　0x00000010 - 　背面液晶に時計を表示<br>
	 * 　　0x00000020 - 　背面液晶モード(ON:nomal OFF:mona)<br>
	 * 　　0x00000040 - 　背面液晶星<br>
	 * 　　0x00000100 - 　xxxﾊｲｺﾝﾄﾗｽﾄxxx　廃止<br>
	 * 　　0x00000200 - 　しおりの自動更新　自動しおり機能<br>
	 * 　　0x00000400 - 　壁紙の非表示<br>
	 * 　　0x00000800 - 　ﾒｰﾙ通知機能<br>
	 * 　　0x00001000 - 　ｾｯｼｮﾝを毎回切断<br>
	 * 　　0x00002000 - 　透明削除対策<br>
	 * 　　0x00004000 - 　ｷｬｯｼｭ情報の表示<br>
	 * 　　0x00008000 - 　1ｽﾚずつｽｸﾛｰﾙ(スレ覧)<br>
	 * 　　0x00010000 - 　スレ一覧時、選択キーで最新レスを読む<br>
	 * 　　0x00020000 - 　ﾍﾟｰｼﾞｽｸﾛｰﾙ(スレ覧)<br>
	 * 　　0x00040000 - 　日時表示<br>
	 *　　 0x00080000 - 　レスアンカーのON,OFF
	 * o58 -  配色設定<br>
	 * _59 -  Linklistの使用分<br>
	 * _60 -  Linkfocus<br>
	 * _61 -  Linkバイト数カウント<br>
	 * _62 -  Linkのスタート桁<br>
	 * _63 -  Linkのスタート行<br>
	 * _64 -  Linkrefの使用数<br>
	 * _65 -  <br>
	 * o66 -  ﾌﾞｯｸﾏｰｸの使用数(間の空白も含む)<br>
	 * _67+68 -  ﾌﾞｯｸﾏｰｸ　選択位置 *追加*<br>
	 * o69 -  パケット代の単価(\/1000packet \0.3/packet->\300/1000packet) 0の場合は自動<br>
	 * _70 -  reserved<br>
	 * _71 -  reserved<br>
	 * _72 -  reserved<br>
	 * _73 -  reserved<br>
	 * _74 -  reserved<br>
	 * _75 -  reserved<br>
	 * o76 -  圧縮率<br>
	 * _77 -  スクロール分<br>
	 * _78 -  読み込むもの(0:サーバーが選択 1:レス 2:スレッドリスト 3:一般のダウンロード(text) 4:一般のダウンロード(iMonaヘッダ無し) 5:書き込み)<br>
	 * 　　0x00000001 - 　レス<br>
	 * 　　0x00000002 - 　スレッドリスト<br>
	 * 　　0x00000004 - 　サーバーが選択<br>
	 * 　　0x00000008 - 　一般のダウンロード(text)<br>
	 * 　　0x00000010 - 　書き込み<br>
	 * 　　0x00000100 - 　gzip圧縮指定<br>
	 * 　　0x00000200 - 　iMona用のヘッダ無し(iMona圧縮もなし)<br>
	 * _79 -  取得している板のURLの板番号<br>
	 * o80 -  gzipの圧縮率<br>
	 * _81 -  httpinit内で使用する一時変数<br>
	 * o82 -  スレッドのウエイト<br>
	 * _83 -  キーリピートのキー<br>
	 * o84 -  先読み機能で先読みを行うレス数<br>
	 * _85 -  DivStrの使用分(DivStrで処理している行)<br>
	 * _86 -  現在の行の文字数(iDivStrで使用する変数)<br>
	 * o87 -  数値の設定<br>
	 * _88 -  リンク移動のウエイト キーリピート時のウエイト倍率<br>
	 * _89 -  <br>
	 * o90 -  今までに読んだレスの数<br>
	 * o91 -  パケット代警告<br>
	 * o92 -  スレッド一覧での表示方法<br>
	 * _93 -  通信可能になるまでの待ち時間(単位はミリ秒,phase3.0)<br>
	 * o94 -  7キーの機能<br>
	 * _95 -  キー操作のない時間<br>
	 * _96 -  書込画面のURLのブラウザジャンプ<br>
	 * _97 -  電源&電波ﾏｰｸの表示<br>
	 * _98 -  文字スタイル<br>
	 * _99 -  文字フォント<br>
	 */
	int data[] = new int[100];

	//String strdata[3];
//	String CategoryList[];
//	String BoardList[];
//	String BoardList2[];
	/**
	* stat - フラグその一<br>
	 * 0x0000001 -  初期化完了<br>
	 * 0x0000002 -  リストボックス選択フラグ<br>
	 * 0x0000004 -  キーストップ<br>
	 * 0x0000008 -  キー押し<br>
	 * 0x0000010 -  通信中(通信命令)<br>
	 * 0x0000020 -  通信ステータス１　１のみ：接続準備<br>
	 * 0x0000040 -  通信ステータス２　２のみ：接続完了＆受信中<br>
	 * 0x0000080 -  通信ステータス３　１＋２＋３：受信完了　３のみ：通信失敗<br>
	 * 0x0000100 -  リストボックス表示<br>
	 * 0x0000200 -  データフォルダの表示<br>
	 * 0x0000400 -  設定ダイアログの表示(数字or文字列or色)<br>
	 * 0x0000800 -  既にカテゴリ一覧を取得している<br>
	 * 0x0001000 -  板一覧取得中<br>
	 * 0x0002000 -  カテゴリ選択中<br>
	 * 0x0004000 -  板選択中<br>
	 * 0x0008000 -  スレ一覧のリロード			(**空になった**スレ一覧取得中(初めて取得する場合))<br>
	 * 0x0010000 -  スレ選択中(+スレ続き取得中)<br>
	 * 0x0020000 -  Loading中に操作している	(**空になった**レス取得中)<br>
	 * 0x0040000 -  レス表示中(+レス続き取得中)<br>
	 * 0x0080000 -  通信ストップ要求<br>
	 * 0x0100000 -  上スクロールON<br>
	 * 0x0200000 -  下スクロールON<br>
	 * 0x0400000 -  データフォルダの内容表示<br>
	 * 0x0800000 -  トップの再描画は行わない<br>
	 * 0x1000000 -  全体の再描画<br>
	 * 0x2000000 -  トップだけ再描画<br>
	 * 0x4000000 -  リストだけ再描画<br>
	 * 0x8000000 -  設定時の再描画<br>
	 * 0x10000000 -  通信後の処理中(通信は完了)<br>
	 * 0x20000000 -  未受信のメールあり<br>
	 * 0x40000000 -  Loading中の操作は不可能<br>
	 * 0x80000000 -  Loading中の操作は不可能<br>
	 */
	int stat;
	/**
	* stat2 - フラグその二<br>
	 * 0x0000001 -  設定<br>
	 * 0x0000002 -  表示設定<br>
	 * 0x0000004 -  操作設定<br>
	 * 0x0000008 -  通信設定<br>
	 * 0x0000010 -  色の設定<br>
	 * 0x0000020 -  その他		/xxx/メニューリスト読み込み済みフラグ/xxx/<br>
	 * 0x0000040 -  レス番号指定モード<br>
	 * 0x0000080 -  AA MODE<br>
	 * 0x0000100 -  数字モード<br>
	 * 0x0000200 -  １文字モード<br>
	 * 0x0000400 -  色設定モード<br>
	 * 0x0000800 -  レス番号指定モードで-を表示する フラグがたっていない:-有り フラグがたっている:-無し<br>
	 * 0x0001000 -  確認表示(何かキーを押すと消える)<br>
	 * 0x0002000 -  数字設定での最も低い数字を1にするフラグ<br>
	 * 0x0004000 -  Function(アスタリスクが押されている)<br>
	 * 0x0008000 -  パケット代の集計<br>
	 * 0x0010000 -  ブックマークの表示<br>
	 * 0x0020000 -  ブックマークからスレッドへジャンプしたことを示すフラグ<br>
	 * 0x0040000 -  ブックマークの編集中<br>
	 * 0x0080000 -  ｻｰﾊﾞｰ設定編集中フラグ<br>
	 * 0x0100000 -  SeparateStringで分割するというフラグ<br>
	 * 0x0200000 -  SeparateStringで改行コードで改行したというフラグ<br>
	 * 0x0400000 -  左スクロールON<br>
	 * 0x0800000 -  右スクロールON<br>
	 * 0x1000000 -  インフォメーション表示<br>
	 * 0x2000000 -  DOJAスクロールサポート<br>
	 * 0x4000000 -  ｽﾚｯﾄﾞ検索画面<br>
	 * 0x8000000 -  検索結果表示中フラグ<br>
	 * 0x10000000 -  拡張ｵﾌﾟｼｮﾝ編集中フラグ<br>
	 * 0x20000000 -  書き込み画面表示中フラグ<br>
	 * 0x40000000 -  makeTLで初期化を行う<br>
	 * 0x80000000 -  makeResで初期化を行う<br>
	 */
	int stat2;

	/**
	 * stat3 - フラグその三<br>
	 * 0x0000001 -  LinkON(同じスレッド内)<br>
	 * 0x0000002 -  LinkON(URL)<br>
	 * 0x0000004 -  LinkON(URL)でジャンプしたことを示すフラグ<br>
	 * 0x0000008 <br>
	 * 0x0000010 -  ＤＬしたレス・スレッドリストを最後から見ることを示すフラグ<br>
	 * 0x0000020 -  ブックマークから板へジャンプしたことを示すフラグ<br>
	 * 0x0000040 -  「戻る」で戻っていることを示すフラグ<br>
	 * 0x0000080 -  先読みをすることを示すフラグ 廃止：板一覧が無効(再読込が必要)になったことを示すフラグ<br>
	 * 0x0000100 -  参照元を保存しない<br>
	 * 0x0000200 -  参照元を保存した<br>
	 * 0x0000400 -  ブックマークのメニュー表示<br>
	 * 0x0000800 -  通信機能使用中<br>
	 * 0x0001000 -  URL指定画面<br>
	 * 0x0002000 -  テキストボックス<br>
	 * 0x0004000 -  書込画面のURL<br>
	 * 0x0008000 -  キャッシュが存在する<br>
	 * 0x0010000 -  スクロール実績<br>
	 * 0x0100000 -  thread共有用：板一覧取得<br>
	 * 0x0200000 -  thread共有用：キーリピート<br>
	 * 0x0400000 -  <<接を<<蓄にする<br>
	 * 0x0800000 -  接>>を蓄>>にする<br>
	 * 0x1000000 -  描画中(負荷が高い)<br>
	 * 0x2000000 -  ブックマークのエクスポート レスを描画してからまだ一度もキーが押されていない<br>
	 * 0x4000000 -  ブックマークのインポート<br>
	 * 0x8000000 -  ブックマークの特別メニュー表示<br>
	 */
	int stat3;

	/**
	 * stat4 - フラグその四<br>
	0x0000001 - <br>
	0x0000002 - 通信頻度制限を越えている(phase3.0)<br>
	0x0000004 - APDATA検出用フラグ<br>
	0x0000008 - <br>
	0x0000010 - iアプリで板一覧の保存を要求するフラグ(GraphicsMIDP2DOJA内で要求してbrdinitで保存)<br>
	0x0000020 - <br>
	0x0000040 - <br>
	0x0000080 - <br>
	0x0000100 - 7キーの機能(ｽﾚ覧)設定中<br>
	0x0000200 - 7キーの機能(ｽﾚ覧)実行中<br>
	0x0000800 - <br>
	0x0001000 - <br>
	0x0002000 - <br>
	0x0004000 - ｸｯｼｮﾝﾘﾝｸ編集中<br>
	0x0008000 - 0キーの機能設定中<br>
	0x0010000 - 0キーの機能実行中<br>
	0x0020000 - NGword編集中<br>
	0x0040000 - httpinitでの引数6番目用、書き込みフラグ<br>
	 */
	int stat4;

	/**　詳細不明。　たぶんdata変数のフラグを呼ぶための補助変数かと思われる。*/
	int Powtable[] = { 1, 10, 100, 1000, 10000, 100000};

	/**
	 * 長期的な保存が必要なString変数用の配列
	 * 00 - サーバのURL<br>
	 * 01 - 板のURL(空の場合はまだ取得されていない)<br>
	 * 02 - setstr<br>
	 * 03 - 表示しているレスの名前部分<br>
	 * 04 - httpstr<br>
	 * 05 - sendstr<br>
	 * 06 - strtmp(main thread)<br>
	 * 07 - info文字列<br>
	 * 08 - 読み込む板・スレッドのタイトル<br>
	 * 09 - 現在表示している板・スレッドのタイトル<br>
	 * 10 - 読み込み中の板・スレッドのタイトル<br>
	 * 11 - 通信エラーの内容<br>
	 */
	String strdata[] = new String[20];

	/** 通信などに使うスレッド */
	Thread thread;

	/** iMonaで描画する全ての文字列の為のフォント。これをいじくれば文字形式も変わる */
	Font font;

	/** 壁紙用イメージ */
	Image wallpaper = null;

	//バックバッファ
	//Image backbuf;
	//Graphics bg;
	//ﾒﾆｭｰ構造
	//String MenuStr[] = { "板一覧", "ﾌﾞｯｸﾏｰｸ", "設定", "終了"};
	//	String ThreadMenu[] = { "このｽﾚを見る", "ﾌﾞｯｸﾏｰｸに登録", "ｽﾚ一覧の再読込", "板一覧に戻る"};
	//		String ResMenu[] = { "一覧に戻る", "最新ﾚｽ","ﾚｽ番号", "ﾌﾞｯｸﾏｰｸ", "ﾌﾞｯｸﾏｰｸに登録", "設定", "ﾒﾆｭｰを閉じる"};
	//	String SettingMenu[] = { "X板ﾘｽﾄの更新", "設定の保存", "表示・操作",/* "X配色",*/ "通信設定", "!初期化!", "戻る"};
	//		String ViewSetMenu[] = { "文字ｻｲｽﾞ", "行間", "ｽｸﾛｰﾙ量", "ｵｰﾄｽｸﾛｰﾙ", "X壁紙", "戻る"};
//			String ColSetMenu[] = { "背景色", "文字色", "選択色", "枠色/濃", "枠色/薄", "戻る"};
			//String OpeSetMenu[] = { "ﾍﾟｰｼﾞｽｸﾛｰﾙｷｰ" };
	//		String ComSetMenu[] = { "1回に読むｽﾚ数", "1回に読むﾚｽ数", "X圧縮率", "戻る"};
//	String StrList[9][] = { "名無しさん", "い", "う", "ん", "ttp://"};
	//String StrList[][] = new String[20][];
	/**
	 * メニュー項目用String配列
	 */
	String StrList[][] = {
	/*00*/	{},//temporary
	/*01*/	{"板一覧", "ﾌﾞｯｸﾏｰｸ", "URL指定"/*, "全ｽﾚｯﾄﾞ検索*/, "設定", /*"ﾃﾞｰﾀﾌｫﾙﾀﾞ"*//*"BGM",*/ "終了"},
	/*02*/	{/*"このｽﾚをみる", */"一覧に戻る", "最新ﾚｽ", "ﾚｽ番指定", "1のみ取得", "ﾌﾞｯｸﾏｰｸに登録", "ｽﾚｯﾄﾞを立てる", "再読込", "ｷｬｯｼｭｸﾘｱ", "通信の詳細", /*"ﾒﾆｭｰを閉じる"*/"閉じる"},
	/*03*/	{"一覧に戻る", "最新ﾚｽ", "ﾚｽ番指定", "書込",/*"ここにレス",*/ "書込画面のURL", "ﾌﾞｯｸﾏｰｸ", "ﾌﾞｯｸﾏｰｸに登録", "設定", "ﾃｷｽﾄﾎﾞｯｸｽ", "ﾃｷｽﾄﾎﾞｯｸｽ(URL)", "ｷｬｯｼｭｸﾘｱ", "AAS", /* "ｽﾚｯﾄﾞの詳細",*/ "通信の詳細", /*"ﾒﾆｭｰを閉じる"*/"閉じる"},
	/*04*/	{"板ﾘｽﾄ更新", "設定の保存", "表示設定", "操作設定", "通信設定", "その他", "戻る"},
	/*05*/	{"文字ｻｲｽﾞ","文字ｽﾀｲﾙ","文字ﾌｪｲｽ", "行間", "日時表示", "時刻表示", "秒表示", /*"ｽﾚ一覧のﾒﾆｭｰ数", */"背面液晶", "ﾊｲｺﾝﾄﾗｽﾄ", "壁紙非表示", "ｷｬｯｼｭ情報の表示", "ｽﾚ情報の表示", "電池と電波ﾏｰｸ", "戻る"},
	/*06*/	{"ｽｸﾛｰﾙ量", "ｽｸﾛｰﾙ方法(ｽﾚ覧)", "ｵｰﾄｽｸﾛｰﾙ", "SO用ｽｸﾛｰﾙ処理", "6,4ｷｰでﾚｽ移動(AAMode)", "戻る"},
	/*07*/	{"1回に読むｽﾚ数", "1回に読むﾚｽ数", "最新ﾚｽで読む数"/*, "最新ﾚｽで1も読む"*/, "gzip圧縮", "iMona圧縮", "IDの表示", "時刻の表示", "ﾒｱﾄﾞの表示", "名前の表示", "AAの表示", "URLの表示", "戻る"},
	/*08*/	{"ﾊﾟｹｯﾄ代の累計", "累計のﾘｾｯﾄ", "ﾊﾟｹｯﾄ代の単価", "ﾊﾟｹｯﾄ代警告", "先読み機能", "自動しおり機能", "ﾒｰﾙ着信の通知", "透明削除対策", "最新ﾚｽ表示(ｽﾚ覧)", "7ｷｰの機能(ｽﾚ覧)", "7ｷｰの機能(ﾌﾞｸﾏ覧)", "7ｷｰの機能(ﾚｽ覧)", "0ｷｰの機能", "ｻｰﾊﾞｰ設定", "拡張ｵﾌﾟｼｮﾝ", "ｸｯｼｮﾝﾘﾝｸ", "書込画面のURL", "常にアンカーレス", /*"ｽﾚｷｰﾘﾋﾟｰﾄ速度",*/"ｳｴｲﾄ", "NGﾜｰﾄﾞ(未実装)","ﾊﾞｸﾞ報告",/* "!ﾌﾞｸﾏ初期化!",*/ "!初期化!", "ﾒﾓﾘ情報", "戻る"},

	/*09*/	{},
	/*10*/	{"小", "中", "大"},
	/*11*/	{"×", "○"},
	/*12*/	{},
	/*13*/	{},
	/*14*/	{},
	/*15*/	{},
	/*16*/	{"隙間を詰める", "隙間を作る", "ｴｸｽﾎﾟｰﾄ" , "ｲﾝﾎﾟｰﾄ", "全消去"},
	/*17*/	{"最新ﾚｽを読む", "ﾚｽ番指定", "書込" , "書込画面のURL", "ｽﾚｯﾄﾞ検索", "編集", "消去"},
	/*18*/	{"ｽﾚｯﾄﾞ検索", "編集", "消去"},

	/*19*/	{"単位は", "\\/1000pcktです" , "ex)\\0.3/p->300", "0:自動設定"},
	/*20*/	{"設定金額を超え", "ると､警告が表示" , "されます｡"},
	/*21*/	{"ここで指定した", "ﾚｽ数前になると", "先読みをします", "0:先読みしない"},
	/*22*/	{"ﾚｽを読み込むと", "ﾌﾞｯｸﾏｰｸを自動", "で更新します｡"},
	/*23*/	{"ﾒｰﾙ着信->中断", "iappli x504,5i", "ez+ phase2.5用"},
	/*24*/	{"透明削除時に", "最新ﾚｽを読み", "込みます｡"},
	/*25*/	{"0を指定すると", "1回に読むﾚｽ数", "と同じ値を使用", "します｡"},
	/*26*/	{"x505i,japp用", "0:圧縮なし", "容量:1 > 9", "推奨値:6"},
	/*27*/	{"1:非可逆のみ", "2:未ｻﾎﾟｰﾄ", "3:圧縮なし"},
	/*28*/	{"1:全部表示する", "2:AAのみ消去", "3:AAのみ消去2", "4:2+全消去", "5:3+〃"},
	/*29*/	{"1:全部表示する", "2:省略する", "3:2ch内のみ表示", "4:完全消去"},
	/*30*/	{"0:無し", "1:Mona 2:Nomal", "3:1+星 4:2+星"},
	/*31*/	{"選択ｷｰで最新ﾚｽ", "を読みます｡"},
	/*32*/	{"0:通常", "1:1ｽﾚずつｽｸﾛｰﾙ", "2:ﾍﾟｰｼﾞｽｸﾛｰﾙ"},

	/*33*/  {"○:ﾌﾞﾗｳｻﾞを起動", "×:ｱﾄﾞﾚｽを表示"},
	/*34*/  {"ﾐﾘ秒で指定して", "ください｡", "0:非表示"},
	/*35*/  {"普","太","斜"},
	/*36*/  {"0:ｼｽﾃﾑ","1:等幅","2:ﾌﾟﾛﾎﾟｰｼｮﾅﾙ"},
	/*37*/  {"文字ｻｲｽﾞ変更(常)","非ｸｯｼｮﾝﾘﾝｸｼﾞｬﾝﾌﾟ(ﾚｽ)","自動しおりON,OFF(常)","ﾌﾞｯｸﾏｰｸ登録(ﾚｽ,ｽﾚ)","板ﾘｽﾄ更新"},
	/*38*/  {"○:常にﾚｽｱﾝｶｰ", "×:ﾚｽｱﾝｶｰを付けない"}
		};
	//00 テンポラリ
	//01 MenuStr
	//02 	ThreadMenu
	//03 		ResMenu
	//04 SettingMenu
	//05 	ViewSetMenu
	//06 	ComSetMenu
	//07 	OtherSetting
	//08  <reserved> color setting?
	//09 CompData
	//10 SetCharSize
	//11 SetChoose
	//12 CategoryList
	//13 BoardList
	//14 setcmstr
	//15 info
	//16
	//17
	//18
	//19


	/**
	 * 携帯絵文字用配列
	 */
	String Emoji[] = {"\uE301 ", "\uE101 ", "\uE024 ", "ID:"};
//	byte Emoji[][] = {
//		{-29/*0xE3*/, 0x01, 0x20},
//		{-31/*0xE1*/, 0x01, 0x20},
//		{-32/*0xE0*/, 0x24, 0x20}
//		};

	//ダブルバッファリング
	//Graphics g;	//バックバッファ
	//Image    img2;
	Calendar calendar;

	HttpConnection co = null;
	
	/**
	 * iMona@zuzu専用変数保存領域
	 * 0.レス番号指定の保存
	 */
	int zuzu[] = new int[10];


	Command command[] = new Command[10];

	/**
	 * 起動直後の処理。コマンドボタンの設定、壁紙の表示、画面の横幅縦幅の設定、設定読み込み、配色設定などを行う
	 * @param p iMona
	 */
	public mainCanvas(iMona p){

		int key = 0;
		int i;
		parent = p;
		calendar = Calendar.getInstance();
		zuzu[0] = 0;


		command[0] = new Command("ﾒﾆｭｰ",1/*Command.SCREEN*/,0);
		command[1] = new Command("消去",1/*Command.SCREEN*/,1);
		command[2] = new Command("取消",1/*Command.SCREEN*/,2);
		command[3] = new Command("編集",1/*Command.SCREEN*/,0);
		command[4] = new Command("更新",1/*Command.SCREEN*/,0);
		command[5] = new Command("ﾒｰﾙ",1/*Command.SCREEN*/,5);
		command[6] = new Command("戻る",1/*Command.SCREEN*/,5);
		command[7] = new Command("検索",1/*Command.SCREEN*/,5);
		command[8] = new Command("実行",1/*Command.SCREEN*/,0);
		command[9] = new Command("登録",1/*Command.SCREEN*/,4);
		//command[9] = new Command("ｽﾚ戻",Command.SCREEN,0);
		//command[10] = new Command("板戻",Command.SCREEN,0);
		//command[11] = new Command("BM戻",Command.SCREEN,0);
		setCommandListener(this);
		ListBox = StrList[1];
		//data[19] = ListBox.length;
		stat |= 0x0100;
		width = getWidth();
		height = getHeight();
		//ダブルバッファリング
		//backbuf = Image.createImage( width, height);
		//bg = backbuf.getGraphics();

		data[50] = width / 2;
		data[51] = height / 2;
		data[52] = width / 2;
		data[53] = height / 2;
		try {
			//壁紙
			data[37] = 0;	data[38] = 0;
			if(width < 240){
				wallpaper = Image.createImage("/m.png");
			} else {
				wallpaper = Image.createImage("/m2.png");
				data[37] = +10;
				data[38] = +5;
			}



			//壁紙
			data[37] += width / 2 - wallpaper.getWidth() / 2;	//default x
			//data[38] = 50;	//default y
			data[38] += height - 10 - wallpaper.getHeight();	//default y
		} catch(Exception e){
		}
		Load();	//レコードストアから読み込み
		//配色
		ColScm = ColPreset[data[58]];
		//if((data[57] & 0x00000100) != 0){
		//	data[57] ^= 0x00000100;
		//	data[58] = 1;
		//	ColScm = ColPreset[1];
		//}
		// else {
		//	data[58] = 0;
		//	ColScm = ColPreset[0];
		//}
		//System.out.println("gc " + (Runtime.getRuntime()).freeMemory());
		System.gc();	//ガベージコレクション
		stat |= 0x1000000;	//再描画
		// スレッドの設定
		Thread thread = new Thread(this);
		thread.start();
//#ifdef 	//
//#else
//		StrList[12] = CategoryList;	//カテゴリ
//		StrList[13] = BoardList;	//板
//		stat |= 0x0000800;	//カテゴリリスト読み込み済みフラグ
//		data[44] = CATNUM;		//板をすべて読み込み終わってる
	//	if(StrList[12].length == 0){	//まだ板一覧がダウンロードされていない場合
	//		try {
	//			httpinit(0,0,0,0,0);//dlbrdlist();
	//		} catch(Exception e){
	//			strdata[7] = e.toString();
	//			stat2 |= 0x0001000;
	//		}
	//	}
//#endif
	}















//#ifdef 	//
/**
* 板一覧の読み込み
*/
	public final void brdinit(){
		int i;


		try{
//#ifdef 	//
			if(brdarray != null){
				//板一覧をメモリから読み込み
				String[] tmp = split(new String(brdarray), '\n');
				i_length = tmp.length;
				for(i = 0; i < i_length;i++){
					if(tmp[i].length() == 0){break;}
				}
				//カテゴリの数分メモリを取得する
				StrList[12] = new String[i];//カテゴリ
				StrList[13] = new String[i];//板
				System.arraycopy(tmp, 0, StrList[12], 0, i);
				stat |= 0x0000800;	//カテゴリリスト読み込み済みフラグ
				System.arraycopy(tmp, i+1, StrList[13], 0, tmp.length - i - 1);
				tmp = null;
			} else {
/*
				//カテゴリの数分メモリを取得する
				StrList[12] = new String[CATNUM];//カテゴリ
				StrList[13] = new String[CATNUM];//板
*/
				String[] tmp = new String[100];

//#ifdef DOJA	//DOJA
//				ByteArrayInputStream in = new ByteArrayInputStream(dlarray);
//#else
				InputStream in;
//				if(brdarray != null){
//					in = new ByteArrayInputStream(brdarray);
//				} else {
					//板一覧をリソースから読み込む
					in = getClass().getResourceAsStream("/cat.txt");
//				}
//#endif
				ByteArrayOutputStream out = new ByteArrayOutputStream(300);
				byte b[] = new byte[1];
				i = 0;
				while(true){
					int len;
					len = in.read(b);
					if(len < 0 || b[0] == '\n'){
						tmp[i] = out.toString();
						i++;
						out.reset();
						if(len < 0 || i == tmp.length/*誤動作防止*/){
							break;
						}
					} else {
						out.write(b);
					}
				}
//#ifdef DOJA	//DOJA
//				in.read(b);
//#else
//				if(brdarray != null){
//					in.read(b);
//				} else {
					in.close();
//				}
//#endif
				StrList[12] = new String[i];//カテゴリ
				System.arraycopy(tmp, 0, StrList[12], 0, i);
				stat |= 0x0000800;	//カテゴリリスト読み込み済みフラグ
				out.reset();
//#ifdef DOJA	//DOJA
//				in = this.getClass().getResourceAsStream("resource:///brd.txt");
//#else
				if(brdarray == null){
					in = getClass().getResourceAsStream("/brd.txt");
				}
				//data[44] = 0;
				i = 0;
				while(true){
					int len;
					len = in.read(b);
					if(len < 0 || b[0] == '\n'){
						//StrList[13][data[44]] = out.toString();
						//data[44]++;
						tmp[i] = out.toString();
						i++;
						out.reset();
						if(len < 0 || i/*data[44]*/ == tmp.length/*StrList[13].length*//*誤動作防止*/){
							break;
						}
					} else {
						out.write(b);
					}
				}
				in.close();

				StrList[13] = new String[i];//板
				System.arraycopy(tmp, 0, StrList[13], 0, i);
			}

		} catch(Exception e){
		}
		System.gc();	//ガベージコレクション

	}
//#endif
	//コマンドイベント
	/**
	 * コマンドイベント
	 * @param c Command
	 * @param s Displayable
	 */
	public final synchronized void commandAction(Command c,Displayable s) {
		commandAction(c);
	}

	/**
	 * コマンドイベント
	 * @param c Command
	 */
	public final synchronized void commandAction(Command c) {
		int i;
		if((stat & 0x0000004) != 0){	//キーストップがかかっていたら
			return;
		}
		data[95] = 0;
		if((stat3 & 0x0000400) != 0){//ﾌﾞｯｸﾏｰｸメニュー
			i = data[67] + data[68];
		} else {
			i = data[10] + data[11];
		}
		if(c == command[0]){	//メニュー KEY_SOFT1
			//if((stat2 & 0x0010000) != 0 && (stat2 & 0x0040000) == 0){//ﾌﾞｯｸﾏｰｸ
			//	ShowBookMarkMenu();
			//} else
			//System.out.println("stat2:" + stat2 + " 1:" + stat);
			if((stat2 & 0x0010000) != 0 && (stat2 & 0x0040000) == 0){//ﾌﾞｯｸﾏｰｸ
				if((stat3 & 0x0000400) == 0){//ﾌﾞｯｸﾏｰｸメニュー
					//ShowBookMarkMenu();
					showBookMark(1);
				}
			} else if((stat2 & 0x000070F) == 0 && (stat & 0x450000) != 0){
				if( (stat & 0x0100) != 0 ){	//リストボックス
					stat ^= 0x0100;
				} else {
					stat |= 0x0100;
					data[10] = data[11] = 0;

					//if((stat2 & 0x0020000) != 0){
					//	ListBox[0] = "ﾌﾞｯｸﾏｰｸに戻る";
					//}
					removeCommand(command[6]);
					removeCommand(command[0]);
				}
				stat |= 0x1000000;	//画面更新
			}
		} else if(c == command[6] && (stat & 0x10000010) == 0/* || c == command[9] || c == command[10] || c == command[11]*/){	//戻る

			if(data[64] > 0){	//参照元がある場合
				stat3 |= 0x0000140;
				//stat3 |= 0x0000040;	//戻るでアクセスしている
				//stat3 |= 0x0000100;	//参照元を保存しない
				strdata[8] = "?";
				if(Linkref[data[64]*3+1] > 0){
					i = Linkref[data[64]*3+2] + data[1] - 1;
				} else {
					i = Linkref[data[64]*3+2] + data[0] - 1;
				}
				httpinit( 2, Linkref[data[64]*3], Linkref[data[64]*3+2],i,Linkref[data[64]*3+1]);
				thttpget();	//httpgetdata()を新規スレッドで動作
			} else if((stat2 & 0x0020000) != 0){	//ブックマークからアクセスしてた場合(直接スレッドへのブックマーク)
				stat2 &= ~0x0020000;
				//data[77] = 0;
				stat &= ~0x00040000;	//スレを見てるフラグを消去
				showBookMark(0);
			} else if((stat & 0x10000) != 0){	//スレッドリストを表示している場合
				backfromfirst();
				//stat |= 0x1000000;	//画面更新
			} else if( (stat & 0x40000) != 0 ){	//ﾚｽ見てる時
				data[77] = 0;
				stat &= ~0x00040000;	//スレを見てるフラグを消去
				removeCommand(command[0]);
				removeCommand(command[6]);
				data[10] = 0;	data[11] = 0;
				ListBox = StrList[1];
				stat |= 0x0000100;
				stat |= 0x1000000;	//画面更新
			}
		} else if(c == command[4]){	//更新
			if((stat2 & 0x0080000) != 0){	//ｻｰﾊﾞｰ設定編集中フラグ
				i = choice.getSelectedIndex();
				if(i == 0){
					server = server_url[0];
				} else if(i == 1){
					server = server_url[1];
				} else if(i == 2){
					server = server_url[2];
				} else {
					server = tfield.getString();
				}
				//SaveSetting();
				stat2 &= ~0x0080000;
			} else if((stat2 & 0x10000000) != 0){	//拡張ｵﾌﾟｼｮﾝ編集中フラグ
				extendedoption = tfield.getString();
				//SaveSetting();
				stat2 &= ~0x10000000;
			}  else if((stat4 & 0x00004000) != 0){	//ｸｯｼｮﾝﾘﾝｸ編集中フラグ
				cushionlink = tfield.getString();
				//SaveSetting();
				stat4 &= ~0x00004000;
			} else if((stat4 & 0x00020000) != 0){
				ngword = tfield.getString();
				//SaveSetting();
				stat4 &= ~0x00020000;
			}
			else {
				EditBookMark( i + 1, btitle.getString(), Integer.parseInt(bboard.getString()), Integer.parseInt(bthread.getString()), Integer.parseInt(bres.getString()));
				//	strdata[7] = "更新失敗";
				//	stat2 |= 0x0001000;
				inputForm = null;
				stat2 &= ~0x0040000;
			}
			disp.setCurrent(this);
			stat |= 0x1000000;	//画面更新
		} else if(c == command[8]){	//実行
			if((stat2 & 0x4000000) != 0){//ｽﾚｯﾄﾞ検索画面
				inputForm = null;
				disp.setCurrent(this);
				//stat |= 0x8000;	//スレ一覧ＤＬ中
				stat2 |= 0x8000000;	//検索結果表示中フラグ
				strdata[8] = ListBox[data[10] + data[11]];
				strdata[6] = tfield.getString();
				searchtext = strdata[6];
				//if((stat3 & 0x0000400) == 0){//ﾌﾞｯｸﾏｰｸメニュー
				//	httpinit(4,(data[22] + data[23]) * 100 + data[10] + data[11],0,0,0);
				//} else {
				//	httpinit(4,BookMarkData[i * 3],0,0,0);
				//}

				httpinit(4,Linkref[0],1,0,0);
				thttpget();	//httpgetdata()を新規スレッドで動作
				stat2 &= ~0x4000000;
			} else if((stat3 & 0x0001000) != 0){//URL指定画面
				disp.setCurrent(this);
				//strdata[6] = tfield.getString();
				stat3 |= 0x0000100;
				//httpinit(5, 0, 1, 0, 0);
				//thttpget();	//httpgetdata()を新規スレッドで動作
				Link(tfield.getString(), 0);
				thttpget();	//httpgetdata()を新規スレッドで動作
				stat3 &= ~0x0001000;
			} else if((stat2 & 0x20000000) != 0){	//書込画面
				disp.setCurrent(this);
				//編集内容のｾｰﾌﾞ
				name = btitle.getString();
				mail = bres.getString();
				bodytext = bboard.getString();
				if((stat2 & 0x0010000) != 0){//ﾌﾞｯｸﾏｰｸ
					httpinit(7, BookMarkData[i * 3], 0, 0, BookMarkData[i * 3 + 1]);
				} else {
					httpinit(7, nCacheBrd[nCacheIndex]/*data[3]*/, 0, 0, nCacheTh[nCacheIndex][0]/*data[2]*/);
				}
				stat2 &= ~0x20000000;
			} else if((stat3 & 0x4000000) != 0){	//ｲﾝﾎﾟｰﾄ
				/*String test = tobox.getString();
				if(test.indexOf("iB2") == 0){

				}*/
				String[] buf = split(tbox.getString(), ',');
				if(buf[0].equals("iB2")){
					for(i = 1; i < buf.length;i+=5){
						if(EditBookMark(0, buf[i], Integer.parseInt(buf[i+1],36), Integer.parseInt(buf[i+2],36), Integer.parseInt(buf[i+3],36)) < 0){
							break;
						}
					}
				}else if(buf[0].equals("iB")){
					for(i = 1; i < buf.length;i+=4){
						if(EditBookMark(0, buf[i], Integer.parseInt(buf[i+1],36), Integer.parseInt(buf[i+2],36), Integer.parseInt(buf[i+3],36)) < 0){
							break;
						}
					}
				}
				disp.setCurrent(this);
				stat3 &= ~0x4000000;
			}/* else if((stat4 & 0x0008000) != 0){ //全消去
				i = choice.getSelectedIndex();
				if(i == 0){
					server = server_url[0];
				} else if(i == 1){
					server = server_url[1];
				}
				//SaveSetting();
				stat4 &= ~0x0008000;
			}*/
		} else if(c == command[7]){	//検索

			tfield = new LocalizedTextField("ｷｰﾜｰﾄﾞ",searchtext,20,LocalizedTextField.ANY);
			inputForm = new Form("= ｽﾚｯﾄﾞ検索 =");
			if((stat3 & 0x0000400) != 0){//ﾌﾞｯｸﾏｰｸメニュー
				inputForm.append(new StringItem("板番号:" + BookMarkData[i * 3],""));
				Linkref[0] = BookMarkData[i * 3];
			} else {
				inputForm.append(new StringItem("板名:" + ListBox[i],""));
				Linkref[0] = (data[22] + data[23]) * 100 + i;
			}
			inputForm.append(tfield);

			inputForm.addCommand(command[8]);
			inputForm.addCommand(command[2]);

			inputForm.setCommandListener(this);

			disp.setCurrent(inputForm);

			stat2 |= 0x4000000;

		} else if(c == command[3]){	//編集
			if((stat2 & 0x0010000) != 0 && (stat2 & 0x0040000) == 0){//ﾌﾞｯｸﾏｰｸ

				btitle = new LocalizedTextField("ﾀｲﾄﾙ",BookMark[i],100,LocalizedTextField.ANY);
				bres = new LocalizedTextField("ﾚｽ番号(1-999)",""+BookMarkData[i * 3 + 2],4,LocalizedTextField.NUMERIC);
				bboard = new LocalizedTextField("板番号(変更不要)",""+BookMarkData[i * 3],8,LocalizedTextField.NUMERIC);
				bthread = new LocalizedTextField("ｽﾚ番号(変更不要)",""+BookMarkData[i * 3 + 1],16,LocalizedTextField.NUMERIC);
				inputForm = new Form("= 編集 =");
				inputForm.append(btitle);
				inputForm.append(bres);
				inputForm.append(bboard);
				inputForm.append(bthread);

				inputForm.addCommand(command[4]);
				inputForm.addCommand(command[2]);

				inputForm.setCommandListener(this);

				disp.setCurrent(inputForm);

				stat2 |= 0x0040000;
			}
		} else if(c == command[9]){//板をﾌﾞｯｸﾏｰｸ登録
			if( (stat & 0x0004000) != 0 ){	//板リストにいるとき
				//strdata[9] = ListBox[data[10]+data[11]];
				//EditBookMark( 0,"[板]" + strdata[9], ((data[22] + data[23]) * 100 + data[10] + data[11]), 0, 0);
				EditBookMark( 0,"[板]" + ListBox[data[10]+data[11]], ((data[22] + data[23]) * 100 + data[10] + data[11]), 0, 0);
			//} else if((stat & 0x0010000) != 0){	//スレ選択中
			//	EditBookMark( 0, CacheBrdData[nCacheIndex]/*ThreadName*/[data[4]], nCacheBrd[nCacheIndex]/*data[3]*/, nCacheTh[nCacheIndex]/*nThread*/[data[4]], 0);
			}
			//stat |= 0x1000000;

/*		} else if(c == command[1]){
			if((stat2 & 0x0010000) != 0){//ﾌﾞｯｸﾏｰｸの消去
				i = data[10] + data[11];
				//if(BookMarkData[i * 3 + 1] != 0){	//未使用でないか
				EditBookMark( - i - 1, "", 0, 0, 0);	//消去
				//}
				//removeCommand(command[1]);
				//removeCommand(command[3]);
			}
			//stat |= 0x1000000;	//画面更新
*/
		} else if(c == command[2]){	//KEY_SOFT2	取消
			///System.out.println("ST:CANCEL");
			if((stat & 0x0000400) != 0){	//設定取り消し
				stat &= ~0x0000400;//if((stat & 0x0000400) != 0){stat ^= 0x0000400;}

				stat2 &= ~0x0000740;//下の4つを結合
				//stat2 &= ~0x0000040;
				//stat2 &= ~0x0000100;//if((stat2 & 0x0000100) != 0){stat2 ^= 0x0000100;}
				//stat2 &= ~0x0000200;//if((stat2 & 0x0000200) != 0){stat2 ^= 0x0000200;}
				//stat2 &= ~0x0000400;//if((stat2 & 0x0000400) != 0){stat2 ^= 0x0000400;}
/*
				data[50] = width / 2;
				data[51] = height / 2;
				data[52] = width / 2;
				data[53] = height / 2;
*/
				stat2 &= ~0x1000000;

				removeCommand(command[2]);
//				if( (stat & 0x10000) != 0 ){	//ｽﾚ選択中の時
//					addCommand(command[9]);
//					addCommand(command[6]);
//				}
			}
			else if((stat2 & 0x0010000) != 0 && (stat2 & 0x0040000) != 0){	//ﾌﾞｯｸﾏｰｸ更新取り消し
				stat2 &= ~0x0040000;
//					form = null;
				disp.setCurrent(this);
			} else if((stat2 & 0x4000000) != 0){	//検索取り消し
				stat2 &= ~0x4000000;
				searchtext = "";
				disp.setCurrent(this);
			} else if((stat2 & 0x0080000) != 0){	//ｻｰﾊﾞｰ設定編集中フラグ
				stat2 &= ~0x0080000;
				disp.setCurrent(this);
			} else if((stat2 & 0x10000000) != 0){	//拡張ｵﾌﾟｼｮﾝ編集中フラグ
				stat2 &= ~0x10000000;
				disp.setCurrent(this);
			} else if((stat4 & 0x00004000) != 0){	//ｸｯｼｮﾝﾘﾝｸ編集中フラグ
				stat4 &= ~0x00004000;
				disp.setCurrent(this);
			} else if((stat4 & 0x00020000) != 0){
				stat4 &= ~0x00020000;
				disp.setCurrent(this);
			} else if((stat3 & 0x0001000) != 0){	//URL指定
				stat3 &= ~0x0001000;
				disp.setCurrent(this);
			} else if((stat3 & 0x0002000) != 0){	//ﾃｷｽﾄﾎﾞｯｸｽ
				stat3 &= ~0x0002000;
				disp.setCurrent(this);
			} else if((stat3 & 0x0004000) != 0){	//書込画面のURL
				stat3 &= ~0x0004000;
				disp.setCurrent(this);
			} else if((stat2 & 0x20000000) != 0){	//書込画面
				//編集内容のｾｰﾌﾞ
				name = btitle.getString();
				mail = bres.getString();
				bodytext = bboard.getString();
				stat2 &= ~0x20000000;
				if((stat4 & 0x0080000) != 0){
					stat4 ^= 0x0080000;
				}
				disp.setCurrent(this);
			} else if((stat3 & 0x2000000) != 0){	//ｴｸｽﾎﾟｰﾄ
				stat3 &= ~0x2000000;
				disp.setCurrent(this);
			} else if((stat3 & 0x4000000) != 0){	//ｲﾝﾎﾟｰﾄ
				stat3 &= ~0x4000000;
				disp.setCurrent(this);
			}
			//setSoftLabel(Frame.SOFT_KEY_2, null);
			if((stat4 & 0x0080000) != 0){
				stat4 ^= 0x0080000;
				keyPressed(KEY_NUM4);
			}
			stat |= 0x1000000;	//画面更新
		}
		//repaint();
	}
	/**
	 * キーリピート
	 */
	public final void keyRepeated(int keyCode) {
		if( keyCode != 0 ){
			int action = getGameAction(keyCode);

			if((action == DOWN || action == UP || keyCode == KEY_NUM8 || keyCode == KEY_NUM2) && (stat & 0x0100) != 0){
				keyPressed(keyCode);
			}

			if((action == LEFT || action == RIGHT || keyCode == KEY_NUM4 || keyCode == KEY_NUM6) && (stat & 0x0040000) != 0){
				//System.out.println("2action:" + action);
				//wait(data[82]);
				//data[70] = 1;
				keyPressed(keyCode);
				//data[70] = 0;
			}
		}
	}
	//キープレスイベント
	/**
	 * キープレスイベント
	 */
	protected synchronized final void keyPressed(int keyCode) {
		int i, j = 0, k;

		data[95] = 0;


		int action = getGameAction(keyCode);
		if((stat & 0x0000004) != 0){	//キーストップがかかっていたら
			if(action == FIRE || keyCode == KEY_NUM5){	//通信のキャンセル
				stat |= 0x0080000;	//通信中断
				try {
					if( (stat & 0x0020) != 0 && (stat & 0x0040) == 0 && co != null){co.close();	co = null;}
				} catch(Exception e) {}
				return;
			} else {
				if((stat2 & 0x0010000) != 0 || (stat & 0x40000000) != 0){//ブックマークorLoading中の操作は不可能
					return;
				} else {
					if(keyCode != KEY_NUM0){
						if((stat3 & 0x0000200) != 0){//参照元を保存していた場合は破棄する
							stat3 &= ~0x0000200;
							if(data[64] > 0){data[64]--;}
						}
						stat |= 0x0020000;
					}
				}
			}
		}
/*
		if((stat2 & 0x0001000) != 0){//確認表示(何かキーを押すと消える)
			return;
		}
*/
		if((stat2 & 0x0001000) != 0){//確認表示(何かキーを押すと消える)
			stat2 ^= 0x0001000;
			strdata[7] = null;
			stat |= 0x1000000;	//画面更新
			return;
		}
		//stat3 &= ~0x2000000;	//レスを描画してからまだ一度もキーが押されていない
//				if((key & 0x080000) != 0 || (key & 0x000100) != 0){//keydown
//				if((key & 0x080100) != 0){//keydown
		if(action == DOWN || keyCode == KEY_NUM8){//keydown
			if((stat & 0x0000400) != 0){	//設定
				if((stat2 & 0x0000040) != 0){//レス番号指定
					if(data[27] > 4){
						data[28] -= Powtable[data[27]-5];
						if(data[28] < 1){data[28] = 1;}
					} else if(data[27] < 4){
						data[29] -= Powtable[data[27]];
						if(data[29] < data[28]){data[29] = 0;}
					} else {
						//data[26] = 1 - data[26];
						stat2 ^= 0x0000800;
					}
				} else if((stat2 & 0x0000100) != 0){//数字
					data[28] -= Powtable[data[27]];
					if((stat2 & 0x0002000) != 0 && data[28] < 1){data[28] = 1;}
					else if(data[28] < 0){data[28] = 0;}
				} else if((stat2 & 0x0000200) != 0 && data[28] > 0){
					data[28]--;
				}
				stat |= 0x8800000;	//画面更新 0x8000000 + 0x0800000
			} else if( (stat & 0x0100) != 0 ){	//リストボックス表示.
				j = data[10] + data[11];

				if((stat2 & 0x0010000) != 0 && (j >= data[66] || (j == data[66]-1 && (stat2 & 0x0004000) != 0)) && (stat3 & 0x0000400) == 0){	//ブックマーク
					data[10] = 0;	data[11] = 0;
				} else if(j < ListBox.length - 1){	//data[10] リスト移動	//data[11] 選択移動
					if(data[11] == (data[13] - 13) / (data[30] + 3) - 1){
						data[10]++;
					} else {
						data[11]++;
					}
					//stat |= 0x4000000;	//リスト画面更新
					//stat |= 0x0800000;	//トップの再描画は必要ない
					//stat |= 0x4800000;	//上二つのセット
//							stat |= 0x1000000;
				} else {
					data[10] = 0;	data[11] = 0;
				}

				if(j != data[10] + data[11] && (stat2 & 0x0014000) == 0x0014000 && (stat3 & 0x0000400) == 0){	//ブックマークの移動
					ChangeBookMark(j , data[10] + data[11]);
				}

				stat |= 0x4800000;	//リスト画面更新+トップの再描画は必要ない
/*					} else if(mode == 0){
			} else if(mode == 1){
				viewy -= 3;//--;
				stat |= 0x1000000;
*/
			} else if( (stat & 0x0450000) != 0 ){//下スクロール
			/*
				if(data[77] < DivStr.length * (data[30] + data[34]) - height + 30 ){
					if( (stat & 0x0000200) != 0 ){stat ^= 0x0008;}
					data[77] += data[35];
					stat |= 0x1000000;	//画面更新
				}*/
/*
				if((data[57] & 0x00008000) != 0 && (stat & 0x0010000) != 0){
					if(data[60] + 1 < data[59]){
						//data[77] += ((CacheBrdData[nCacheIndex][data[60]].getBytes().length+2+GetDigits(nCacheTh[nCacheIndex][data[60]])) / data[42] + 1) * (data[30] + data[34]);
						data[60]++;
						data[77] = ((Linklist[data[60]] / 1000) % 10000) * (data[30] + data[34]);
						stat |= 0x1000000;	//画面更新
//						System.out.println(CacheBrdData[nCacheIndex][data[60]] + " " + CacheBrdData[nCacheIndex][data[60]].getBytes().length + " " + data[42]);
					}
				} else {
*/
					if((stat & 0x0200000) == 0){
						stat3 &= ~0x0010000;	//スクロール実績
						stat |= 0x0200000;//下スクロールON
						data[88] = 0;
						//Scroll();
					}// else
					if( (data[49] & 0x20) != 0){	//SO用ｽｸﾛｰﾙ処理
						Scroll();
					}
//				}
			}
//				} else if((key & 0x020000) != 0 || (key & 0x000004) != 0){//keyup
//				} else if((key & 0x020004) != 0){//keyup
		} else if(action == UP || keyCode == KEY_NUM2){//keyup
			if((stat & 0x0000400) != 0){	//設定
				if((stat2 & 0x0000040) != 0){//レス番号指定
					if(data[27] > 4){
						data[28] += Powtable[data[27]-5];
						if(data[28] > data[26]){data[28] = data[26];}
					} else if(data[27] < 4){
						data[29] += Powtable[data[27]];
						if(data[29] > data[26]){data[29] = data[26];}
						if(data[29] < data[28]){data[29] = data[28];}
					} else {
						//data[26] = 1 - data[26];
						stat2 ^= 0x0000800;
					}
				} else if((stat2 & 0x0000100) != 0){//数字
					data[28] += Powtable[data[27]];
					if(data[28] > data[26]){data[28] = data[26];}
				} else if((stat2 & 0x0000200) != 0 && data[28] < data[26]){
					data[28]++;
				}
				stat |= 0x8800000;	//画面更新 0x8000000 + 0x0800000
			} else if( (stat & 0x0100) != 0 ){	//リストボックス表示
				j = data[10] + data[11];
				if(data[10] == 0 && data[11] == 0){
					if((stat2 & 0x0010000) != 0 && (stat3 & 0x0000400) == 0){	//ブックマーク & !ブックマークメニュー
						//for(i = ListBox.length-1; i > 0; i--){
						//	if(!(BookMark[i].equals(""))){
						//		break;
						//	}
						//}
						//data[11] = i;
						if((stat2 & 0x0004000) != 0){
							data[11] = data[66] - 1;
						} else {
							data[11] = data[66];
							if(data[66] == data[40]){data[11]--;}
						}
					} else {
						data[11] = ListBox.length-1;
					}
					i = (data[13] - 13) / (data[30] + 3) - 1;
					if(data[11] > i){
						data[10] = data[11] - i;
						data[11] = i;
					}
				} else {
					if(data[11] > 0){
						data[11]--;
					} else if(data[10] > 0){
						data[10]--;
					}
					//stat |= 0x4000000;	//リスト画面更新
					//stat |= 0x0800000;	//トップの再描画は必要ない
					//stat |= 0x4800000;	//上二つのセット
				}

				if(j != data[10] + data[11] && (stat2 & 0x0014000) == 0x0014000){	//ブックマークの移動
					ChangeBookMark(j , data[10] + data[11]);
				}
				stat |= 0x4800000;	//リスト画面更新+トップの再描画は必要ない
/*					} else if(mode == 0){
			} else if(mode == 1){
				if(viewy < 0){viewy += 3;}//+;}
				stat |= 0x1000000;
*/
			} else if( (stat & 0x0450000) != 0 ){	//ﾚｽｽｸﾛｰﾙ
					if((stat & 0x0100000) == 0){
						stat3 &= ~0x0010000;	//スクロール実績
						stat |= 0x0100000;//上スクロールON
						data[88] = 0;
						//Scroll();
					}// else
					if( (data[49] & 0x20) != 0){	//SO用ｽｸﾛｰﾙ処理
						Scroll();
					}
//				}
			}
//				} else if((key & 0x010000) != 0 || (key & 0x000010) != 0){//left
//				} else if((key & 0x010010) != 0){//left
		} else if(keyCode == KEY_NUM3){//pagedown
			if((stat & 0x0000100) != 0 && (stat & 0x0000400) == 0 && (stat2 & 0x0004000) == 0){//listbox
				if((stat2 & 0x0010000) != 0 && (stat3 & 0x0000400) == 0){
					i = data[66];
				} else {
					i = ListBox.length;
				}
				data[10] += (data[13] - 13) / (data[30] + 3);
				if(data[10] + data[11] >= i){
					data[10] = 0;	data[11] = 0;
					keyPressed(KEY_NUM2);
				} else if(data[10] + (data[13] - 13) / (data[30] + 3) - 1 > i){
					i = data[11];
					data[10] = 0;	data[11] = 0;
					keyPressed(KEY_NUM2);
					data[11] = i;
				}
				//j = (data[13] - 13) / (data[30] + 3);
				//for (i = 0; i < j; i++) {
			} else {
				i = (data[85] + 5/*2*/) * (data[30] + data[34]) - height;
				if(data[77] < i){
					if( (data[49] & 0x20) != 0 || (data[49] & 0x01) == 0 ){stat &= ~0x0200000;}
					data[77] += height - (data[30] + data[34]) * 2;
					if(data[77] > i){data[77] = i;}
					data[60] = -1;
					setLink();
					if(data[60] == -1){data[60] = data[59] - 1;}
				} else {data[60] = data[59] - 1;}
			}
			stat |= 0x1000000;	//画面更新
		} else if(keyCode == KEY_NUM1){//pageup
			if((stat & 0x0000100) != 0 && (stat & 0x0000400) == 0 && (stat2 & 0x0004000) == 0){//listbox
				data[10] -= (data[13] - 13) / (data[30] + 3);
				if(data[10] + data[11] < 0){
					data[10] = 0;	data[11] = 0;
				} else if(data[10] < 0){
					//data[11] = data[10] + data[11];
					data[10] = 0;
				}
				//j = (data[13] - 13) / (data[30] + 3);
				//for (i = 0; i < j; i++) {
			} else {
				if(data[77] > 0 || (data[77] > -data[30] && (stat & 0x0040000) != 0) ){
					if( (data[49] & 0x20) != 0 || (data[49] & 0x01) == 0 ){stat &= ~0x0100000;}
					data[77] -= height - (data[30] + data[34]) * 2;
					if(data[77] <= -data[30] && (stat & 0x0040000) != 0){data[77] = -data[30];	data[60] = 0;}
					else if(data[77] <= 0){data[77] = 0;	data[60] = 0;}
					else {
						setLink();
					}
				} else {data[60] = 0;}
			}
			stat |= 0x1000000;	//画面更新
		} else if(action == LEFT || keyCode == KEY_NUM4){
			//System.out.println(stat + "");
			if((stat4 & 0x0000100) != 0){	//7ｷｰの機能(ｽﾚ覧)設定キャンセル
				stat4 ^= 0x0000100;
				ListBox = StrList[8];
				data[10] = data[19];
				data[11] = 9 - data[19];
				stat |= 0x1000000;	//画面更新
			} else if((stat4 & 0x0000400) != 0){	//7ｷｰの機能(ﾌﾞｸﾏ覧)設定キャンセル
				stat4 ^= 0x0000400;
				ListBox = StrList[8];
				data[10] = data[19];
				data[11] = 10 - data[19];
				stat |= 0x1000000;	//画面更新
			} else if((stat4 & 0x0001000) != 0){	//7ｷｰの機能(ｽﾚ覧)設定キャンセル
				stat4 ^= 0x0001000;
				ListBox = StrList[8];
				data[10] = data[19];
				data[11] = 11 - data[19];
				stat |= 0x1000000;	//画面更新
			}  else if((stat4 & 0x0008000) != 0){	//0ｷｰの機能設定キャンセル
				stat4 ^= 0x0008000;
				ListBox = StrList[8];
				data[10] = data[19];
				data[11] = 12 - data[19];
				stat |= 0x1000000;	//画面更新
			} else if((stat & 0x0000400) != 0){	//設定ダイアログ
				if((stat2 & 0x0000040) != 0){//レス番号指定
					if(data[27] < 8){data[27]++;}
				} else if((stat2 & 0x0000100) != 0){//数字
					if(data[27] < data[29]){data[27]++;}
				}
				stat |= 0x8800000;	//画面更新 0x8000000 + 0x0800000
			} else if((stat3 & 0x0000400) != 0){//ﾌﾞｯｸﾏｰｸメニュー
				stat3 &= ~0x8000400;
				ListBox = tBookMark;
				data[10] = data[67];	data[11] = data[68];
				addCommand(command[0]);
				addCommand(command[3]);
				stat |= 0x1000000;	//画面更新
			} else if( (stat & 0x0100) != 0 ){	//リストボックス表示
				if((stat2 & 0x0010000) != 0){	//ブックマーク表示
					if( (stat & 0x0040000) != 0 ){
						ListBox = StrList[3];
						data[10] = 0;	data[11] = 0;
					} else {
						ListBox = StrList[1];
						data[10] = 0;	data[11] = 1;
					}
					stat2 ^= 0x0010000;
					//setSoftLabel(Frame.SOFT_KEY_2, null);
					removeCommand(command[3]);
					removeCommand(command[0]);
					//removeCommand(command[1]);
					//data[19] = ListBox.length;
				} else if((stat2 & 0x000003E) != 0){	//設定メインメニューに戻る
					/*if((stat2 & 0x0000010) != 0){	//操作設定
						stat2 ^= 0x0000010;
						ListBox = StrList[4];
					} else */

					//stat2 &= ~0x0000020;
					//stat2 &= ~0x0000010;	//通信設定
					//stat2 &= ~0x0000008;	//通信設定
					//stat2 &= ~0x0000004;	//色の設定
					//stat2 &= ~0x0000002;	//表示設定
					stat2 &= ~0x000003E;

					ListBox = StrList[4];
					//data[19] = ListBox.length;
					data[10] = data[20];	data[11] = data[21];
				} else if((stat2 & 0x0000001) != 0){	//設定
					if((stat & 0x40000) != 0){	//レス表示中
						ListBox = StrList[3];
						data[10] = 0;	data[11] = 0;
					} else {
//#ifdef DEBUG
//						System.out.println("okasii? " + stat);
//#endif
						ListBox = StrList[1];
						data[10] = 0;	data[11] = 3;
					}
					//data[19] = ListBox.length;
					stat2 ^= 0x0000001;	//設定解除
				} else if((stat & 0x450000) != 0){	//レス表示中,スレ選択中
					stat ^= 0x0000100;	//ﾒﾆｭｰ解除
					addCommand(command[0]);
					addCommand(command[6]);
				} else if((stat & 0x04000) != 0){	//板選択中の時
					ListBox = StrList[12];
					stat |= 0x2000;	//カテゴリ選択中
					stat ^= 0x4000;	//板選択中解除
					removeCommand(command[7]);
					removeCommand(command[9]);
					//data[19] = ListBox.length;
					data[10] = data[22];data[11] = data[23];	//場所の読み込み
				} else if((stat & 0x02000) != 0){	//カテゴリ選択中の時
					ListBox = StrList[1];
					stat ^= 0x2000;	//カテゴリ選択中解除
					//data[19] = ListBox.length;
					data[10] = 0;data[11] = 0;	//場所の読み込み
//				} else if(data[10] + data[11] == 0){	//メインメニューの時
//				} else {
//					return;
				}

				stat |= 0x1000000;	//画面更新
			} else if( (stat & 0x10000) != 0 ){	//ｽﾚ選択中の時
//				if(data[5] == 0){//一番はじめのスレを選択していた場合
				if(nCacheSt[nCacheIndex] == 1){
					backfromfirst();
//				} else if(data[4] == 0){//読み込んでる中で一番はじめ
				} else {
//					if(data[5] <= data[0]){i = 1;} else {i = data[5] - data[0] + 1;}
					//sendstr = "b=" + data[3] + "&c=s" + (data[5] - data[0] + 1) + "t" + data[5];//m=m
					//httpinit();
					if(nCacheSt[nCacheIndex] <= data[0]){i = 1;} else {i = nCacheSt[nCacheIndex] - data[0];}
					stat3 |= 0x0000010;	//ＤＬしたレス・スレッドリストを最後から見ることを示すフラグ
					stat3 |= 0x0000100;	//参照元を保存しない
					strdata[8] = strdata[9];
//					httpinit(1,nCacheBrd[nCacheIndex]/*data[3]*/,i,data[5],0);
					httpinit(1,nCacheBrd[nCacheIndex]/*data[3]*/,i,nCacheSt[nCacheIndex] - 1,0);
//				} else {
//					data[5]--;
//					data[4]--;
//
//					chkcache();	//キャッシュチェック
//
//					//DivStr = SeparateString(ThreadName[data[5] % data[0]], -1);
//					DivStr = FastSeparateByte(CacheBrdData[nCacheIndex]/*ThreadName*/[data[4]].getBytes());
//					data[85] = DivStr.length;
				}
			} else if( (stat & 0x0440000) != 0 && (stat2 & 0x0000080) != 0 ){	//ﾚｽｽｸﾛｰﾙ
				if((stat2 & 0x0000080) != 0 && (data[49] & 0x100) != 0 && keyCode == KEY_NUM4){
							if( data[6] > 0 ){	//前のレスに戻る
								data[6]--;
								makeRes();
								//DivStr = sepalateString(ResElements[3]);
								//stat |= 0x1000000;	//画面更新
							} else if(nCacheSt[nCacheIndex]/*data[7]*/ == 1) {	//一番初めのレス
								backfromfirst();
							} else {	//前のレスを読み込む
								if(nCacheSt[nCacheIndex]/*data[7]*/ <= data[1]){i = 1;} else {i = nCacheSt[nCacheIndex]/*data[7]*/ - data[1];}
								//sendstr = "b=" + data[3] + "&t=" + nThread[data[5] % data[0]] + "&c=s" + i + "t" + (data[7] - 1);//m=m
								//httpinit();
								stat3 |= 0x0000010;	//ＤＬしたレス・スレッドリストを最後から見ることを示すフラグ
								stat3 |= 0x0000100;	//参照元を保存しない
								strdata[8] = strdata[9];
								httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,i,nCacheSt[nCacheIndex]/*data[7]*/ - 1,nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/);
				
							}
				}else{
					if((stat2 & 0x0400000) == 0){
						stat3 &= ~0x0010000;	//スクロール実績
						stat2 |= 0x0400000;//左スクロールON
						//Scroll();
					}
				}
			} else if((stat & 0x0040000) != 0){	//レスを見てたとき
				if( data[6] > 0 ){	//前のレスに戻る
					data[6]--;
					makeRes();
					//DivStr = sepalateString(ResElements[3]);
					//stat |= 0x1000000;	//画面更新
				} else if(nCacheSt[nCacheIndex]/*data[7]*/ == 1) {	//一番初めのレス
					backfromfirst();
				} else {	//前のレスを読み込む
					if(nCacheSt[nCacheIndex]/*data[7]*/ <= data[1]){i = 1;} else {i = nCacheSt[nCacheIndex]/*data[7]*/ - data[1];}
					//sendstr = "b=" + data[3] + "&t=" + nThread[data[5] % data[0]] + "&c=s" + i + "t" + (data[7] - 1);//m=m
					//httpinit();
					stat3 |= 0x0000010;	//ＤＬしたレス・スレッドリストを最後から見ることを示すフラグ
					stat3 |= 0x0000100;	//参照元を保存しない
					strdata[8] = strdata[9];
					httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,i,nCacheSt[nCacheIndex]/*data[7]*/ - 1,nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/);
				}
			}
//				} else if((key & 0x040000) != 0 || (key & 0x000040) != 0){//right
//				} else if((key & 0x040040) != 0){//right
		} else if(action == RIGHT || keyCode == KEY_NUM6){
			if((stat & 0x0000400) != 0){	//設定ダイアログ
				if((stat2 & 0x0000040) != 0){
					if(data[27] > 4 || (data[27] > 0 && (stat2 & 0x0000800) == 0/*data[26] == 0*/)){data[27]--;}
				} else if((stat2 & 0x0000100) != 0){//数字orレス番号指定
					if(data[27] > 0){data[27]--;}
				}
				stat |= 0x8800000;	//画面更新 0x8000000 + 0x0800000
			} else if((stat2 & 0x0010000) != 0 && (stat2 & 0x0040000) == 0){//ﾌﾞｯｸﾏｰｸ
				if((stat3 & 0x0000400) == 0){//ﾌﾞｯｸﾏｰｸメニュー
					//ShowBookMarkMenu();
					showBookMark(1);
				}
			} else if( (stat & 0x0440000) != 0 && (stat2 & 0x0000080) != 0){	//ﾚｽｽｸﾛｰﾙ&AAModeの場合
				if((stat2 & 0x0000080) != 0 && (data[49] & 0x100) != 0 && keyCode == KEY_NUM6){
						if(data[6] < nCacheTo[nCacheIndex]/*data[8]*/ - nCacheSt[nCacheIndex]/*data[7]*/){	//次のレスに行く
							data[6]++;
							if(data[84] > 0 && data[6] + data[84] == nCacheTo[nCacheIndex] - nCacheSt[nCacheIndex] && nCacheTo[nCacheIndex] < nCacheAll[nCacheIndex]){
								stat3 |= 0x0000080;	//先読みをすることを示すフラグ
	
								stat3 |= 0x0000100;	//参照元を保存しない
								strdata[8] = strdata[9];
								httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,nCacheTo[nCacheIndex]/*data[8]*/ + 1,nCacheTo[nCacheIndex]/*data[8]*/ + data[1],nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/);
	
									//stat |= 0x0020000;	//Loading中のスレ読み
							}
								makeRes();
								//DivStr = sepalateString(ResElements[3]);
								//stat |= 0x1000000;	//画面更新
						} else {	//次のレスを読み込む
							//sendstr = "b=" + data[3] + "&t=" + nThread[data[5] % data[0]] + "&c=s" + (data[8] + 1) + "t" + (data[8] + data[1]);//m=m
							//httpinit();
							stat3 |= 0x0000100;	//参照元を保存しない
							strdata[8] = strdata[9];
							httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,nCacheTo[nCacheIndex]/*data[8]*/ + 1,nCacheTo[nCacheIndex]/*data[8]*/ + data[1],nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/);
						}
					
				}else{
					if((stat2 & 0x0800000) == 0){
						stat3 &= ~0x0010000;	//スクロール実績
						stat2 |= 0x0800000;//右スクロールON
						//Scroll();
					}
				}
			} else if((stat & 0x40000) != 0 && (stat & 0x0100) == 0){
				if(data[6] < nCacheTo[nCacheIndex]/*data[8]*/ - nCacheSt[nCacheIndex]/*data[7]*/){	//次のレスに行く
					data[6]++;
					if(data[84] > 0 && data[6] + data[84] == nCacheTo[nCacheIndex] - nCacheSt[nCacheIndex] && nCacheTo[nCacheIndex] < nCacheAll[nCacheIndex]){
						stat3 |= 0x0000080;	//先読みをすることを示すフラグ

						stat3 |= 0x0000100;	//参照元を保存しない
						strdata[8] = strdata[9];
						httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,nCacheTo[nCacheIndex]/*data[8]*/ + 1,nCacheTo[nCacheIndex]/*data[8]*/ + data[1],nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/);

						//stat |= 0x0020000;	//Loading中のスレ読み
					}
					makeRes();
					//DivStr = sepalateString(ResElements[3]);
					//stat |= 0x1000000;	//画面更新
				} else {	//次のレスを読み込む
					//sendstr = "b=" + data[3] + "&t=" + nThread[data[5] % data[0]] + "&c=s" + (data[8] + 1) + "t" + (data[8] + data[1]);//m=m
					//httpinit();
					stat3 |= 0x0000100;	//参照元を保存しない
					strdata[8] = strdata[9];
					httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,nCacheTo[nCacheIndex]/*data[8]*/ + 1,nCacheTo[nCacheIndex]/*data[8]*/ + data[1],nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/);
				}
			} else if( (stat & 0x10000) != 0 ){	//次のスレに行く
				if((stat2 & 0x8000000) == 0) {	//次のスレを読み込む
					//sendstr = "b=" + data[3] + "&c=s" + (data[5] + 2) + "t" + (data[5] + data[0] + 1);//m=t
					//httpinit();
					stat3 |= 0x0000100;	//参照元を保存しない
					strdata[8] = strdata[9];
					//httpinit(1,nCacheBrd[nCacheIndex]/*data[3]*/,data[5] + 2,data[5] + data[0] + 1,0);
					httpinit(1,nCacheBrd[nCacheIndex]/*data[3]*/,nCacheTo[nCacheIndex] + 1,nCacheTo[nCacheIndex] + data[0],0);
				}
			}

//				} else if(/*center*/(key & 0x100000) != 0 || /*5*/(key & 0x000020) != 0 ){	//選択ボタン
//				} else if( (key & 0x100020) != 0 ){	//選択ボタン
		} else if(action == FIRE || keyCode == KEY_NUM5){
			if((stat & 0x0000400) != 0){	//設定
				removeCommand(command[2]);
			}
			if( (stat & 0x0100) != 0 || (stat4 & 0x0000200) != 0 || (stat4 & 0x0002000) != 0){	//リストボックス表示
				/*if((stat2 & 0x0000010) != 0){	//操作設定
					switch(data[10] + data[11]){
						case 4:	//戻る
							stat2 ^= 0x0000010;
							ListBox = StrList[4];
						break;
					}
					stat |= 0x1000000;	//画面更新
				} else */
				i = data[10] + data[11];
				if((stat3 & 0x0000400) != 0){//ﾌﾞｯｸﾏｰｸメニュー
					
					if((stat & 0x0000400) != 0){	//設定した後
						stat3 ^= 0x0000400;

						i = data[67] + data[68];
						strdata[8] = BookMark[i];
						setResnum(BookMarkData[i * 3], BookMarkData[i * 3 + 1]);
					}
				} else if((stat2 & 0x0010000) != 0){	//ブックマーク
					data[67] = data[10]; data[68] = data[11];
					j = BookMarkData[i * 3 + 2];
					if(j == 0){	//はじめへのブックマーク(1-)
						j = 1;
					}
					strdata[8] = BookMark[i];
					if(BookMarkData[i * 3 + 1] != 0){	//レスへのブックマーク
						//data[43] = i;
						//stat |= 0x0020000;	//レス表示中(+レス続き取得中)
						if(j == -1 || j == 9999){	//最新レスへのブックマーク
							httpinit(2,BookMarkData[i * 3],-1,data[55],BookMarkData[i * 3 + 1]);
						} else {	//レスへのブックマーク
							httpinit(2,BookMarkData[i * 3],j,j + data[1] - 1,BookMarkData[i * 3 + 1]);
						}
					} else if(!(BookMark[i].length() == 0)){	//スレ一覧(板)へのブックマーク
						//data[43] = i;
						//stat |= 0x8000;	//スレ一覧ＤＬ中
						httpinit(1,BookMarkData[i * 3],j,j + data[0] - 1,0);
					}
				} else if((stat4 & 0x0000100) != 0){	//7ｷｰの機能(ｽﾚ覧)設定後
					stat4 ^= 0x0000100;
					data[94] = data[10]+data[11];
					ListBox = StrList[8];
					data[10] = data[19];
					data[11] = 9 - data[19];
					stat |= 0x1000000;	//画面更新
					Bugln("ｽﾚ覧\n");
				} else if((stat4 & 0x0000400) != 0){	//7ｷｰの機能(ﾌﾞｸﾏ覧)設定後
					stat4 ^= 0x0000400;
					data[71] = data[10]+data[11];
					ListBox = StrList[8];
					data[10] = data[19];
					data[11] = 10 - data[19];
					stat |= 0x1000000;	//画面更新
					Bugln("ﾌﾞｸﾏ覧\n");
				} else if((stat4 & 0x0001000) != 0){	//7ｷｰの機能(ｽﾚ覧)設定後
					stat4 ^= 0x0001000;
					data[73] = data[10]+data[11];
					ListBox = StrList[8];
					data[10] = data[19];
					data[11] = 11 - data[19];
					stat |= 0x1000000;	//画面更新
					Bugln("ｽﾚ覧\n");
				}  else if((stat4 & 0x0008000) != 0){	//0ｷｰの機能設定後
					stat4 ^= 0x0008000;
					data[74] = data[10]+data[11];
					ListBox = StrList[8];
					data[10] = data[19];
					data[11] = 12 - data[19];
					stat |= 0x1000000;	//画面更新
					Bugln("0ｷｰ\n");
				} else if((stat2 & 0x0000020) != 0){	//その他
					othermenu(i);
					stat |= 0x1000000;	//画面更新
				} else if((stat2 & 0x0000008) != 0){	//通信設定
					networksetting(i);
					stat |= 0x1000000;	//画面更新
/*						} else if((stat2 & 0x0000004) != 0){	//色の設定
					switch(data[10] + data[11]){
						case 5:	//戻る
							stat2 ^= 0x0000004;
							ListBox = SettingMenu;
							//data[19] = ListBox.length;
							data[10] = data[11] = 0;
						break;
					}
					stat |= 0x1000000;	//画面更新
*/
				} else if((stat2 & 0x0000004) != 0){	//操作設定
					contmenu(i);
					stat |= 0x1000000;	//画面更新
				} else if((stat2 & 0x0000002) != 0){	//表示設定
					//i = data[10] + data[11];
					viewmenu(i);
					stat |= 0x1000000;	//画面更新
				} else if((stat2 & 0x0000001) != 0){	//設定の時
					//data[19] = ListBox.length;
					settingmenu(i);
					data[20] = data[10];	data[21] = data[11];
					data[10] = 0;data[11] = 0;
					stat |= 0x1000000;	//画面更新
				//} else if((stat & 0x0000200) != 0){	//データフォルダの表示
					//-/ReadDataFolder(data[10] + data[11]);
					//-/stat |= 0x0400000;	//データフォルダの表示

					//-/stat &= ~0x0100;//if((stat & 0x0100) != 0){stat ^= 0x0100;}
					//-/data[6] = 0;
					//-/addCommand(command[0]);//メニュー
					//-/stat |= 0x1000000;	//画面更新
				} else if( (stat & 0x40000) != 0 || (stat4 & 0x0002000) != 0){	//ﾚｽ見てる時
					if((stat4 & 0x0002000) != 0){
						i = data[73];
						stat4 ^= 0x0002000;
					}
					strdata[8] = strdata[9];
					resmenu(i,j);
					stat |= 0x1000000;	//画面更新
				} else if( (stat & 0x10000) != 0 || (stat4 & 0x0000200) != 0){	//ｽﾚ選択中の時
					if((stat4 & 0x0000200) != 0){
						i = data[94];
						stat4 ^= 0x0000200;
					}
					strdata[8] = CacheBrdData[nCacheIndex]/*ThreadName*/[data[60]];
					threadmenu(i);
					stat |= 0x1000000;	//画面更新
				} else if((stat & 0x04000) != 0){	//板選択中の時
					//stat ^= 0x4000;	//板選択中解除
					//stat |= 0x8000;	//スレ一覧ＤＬ中
				//	data[3] = (data[22] + data[23]) * 100 + i;
					strdata[8] = ListBox[i];
					/*
					sendstr = "b=" + data[3];//m=t
					if(data[0] != 10){
						sendstr = sendstr + "c=s1t" + data[0];
					}
					httpinit();*/
					httpinit(1,(data[22] + data[23]) * 100 + i/*data[3]*/,1,data[0],0);

				} else if((stat & 0x02000) != 0){	//カテゴリ選択中の時
/*
					while(data[44] <= i){
						try{Thread.sleep(100);}catch (Exception e){}
					}
*/
					ListBox = split( StrList[13][i], '\t');
					stat ^= 0x2000;	//カテゴリ選択中解除
					stat |= 0x4000;	//板選択中
					stat |= 0x1000000;	//画面更新
					data[22] = data[10];data[23] = data[11];	//場所の保存
					//data[19] = ListBox.length;
					data[10] = 0;data[11] = 0;
					addCommand(command[9]);
					addCommand(command[7]);
				} else {	//メインメニューの時
					//i = data[10] + data[11];
					mainmenu(i);
					if(i == 0 || i == 3 ){
						stat |= 0x1000000;	//画面更新
						//data[19] = ListBox.length;
						data[10] = 0;data[11] = 0;
					}
				}
/*					} else if(mode == 0){
			} else if(mode == 1){
*/
			} else if( (stat & 0x10000) != 0 ){	//ｽﾚ選択中の時
				strdata[8] = CacheBrdData[nCacheIndex][data[60]];
				if((data[57] & 0x00010000) != 0){	//最新レスを読む設定の場合
					httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,-1,data[55],nCacheTh[nCacheIndex]/*nThread*/[data[60]]);
					stat |= 0x40000000;	//Loading中の操作は不可能
				} else {
					httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,1,data[1],nCacheTh[nCacheIndex][data[60]]);
				}
			} else if( (stat & 0x40000) != 0 ){	//レスの表示
				Link(null, 0);
			}
			if((stat & 0x0000400) != 0){	//設定
				addCommand(command[2]);
			}

		} else if(keyCode == KEY_NUM7){
			if( (stat & 0x10000) != 0){	//ｽﾚ選択中の時
				stat4 |= 0x0000200;
				keyPressed(KEY_NUM5);
			}

			if( (stat & 0x0040000) != 0){	//ﾚｽ選択中の時
				stat4 |= 0x0002000;
				keyPressed(KEY_NUM5);
			}
			
			if((stat2 & 0x0010000) != 0){//ﾌﾞｯｸﾏｰｸ
				
				if(/*BookMarkData[i * 3 + 1] == 0 && */BookMark[(data[10]+data[11])].length() == 0){	//スレ番号＝0　板が登録されているor空

				}else{
					if(BookMarkData[(data[10]+data[11]) * 3 + 1] == 0){	//板
						
					}else{  //スレッド
						stat4 |= 0x0080000;
						if(data[71] == 2){//書き込み
							data[67] = data[10];	data[68] = data[11];
							//data[10] = 0;	data[11] = 0;
							j = data[67] + data[68];
							i = data[71];
							strdata[8] = BookMark[j];
							bookmarkmenu(i,j);
							stat2 &= ~0x0004000;	//function解除
							//showBookMark(1);
							stat |= 0x1000000;	//画面更新
						}else{
							
							removeCommand(command[3]);
							removeCommand(command[0]);
							//data[67] = data[10];	data[68] = data[11];
							//data[10] = 0;	data[11] = 0;
							//showBookMark(1);
							//removeCommand(command[3]);
							//removeCommand(command[0]);
							//ListBox = StrList[17];
							data[67] = data[10];	data[68] = data[11];
							//data[10] = 0;	data[11] = 0;
							stat3 |= 0x0000400;
	
							
	
							wait(100);
							keyReleased(KEY_NUM5);
						}
						
						
						//stat3 |= 0x0000400;
						//stat |= 0x1000000;	//画面更新
						//wait(1);
						

						//data[71]
						//addCommand(command[3]);
						//removeCommand(command[0]);
						//addCommand(command[0]);
/*						
						stat &= ~0x0300000;//上下スクロールOFF
						stat2 &= ~0x0C00000;//左右スクロールOFF
						j = data[67] + data[68];
						i = data[71];
						k = 0;
						
						if(i == 1 /*| i == 2){
							stat4 |= 0x0080000;
						}*/
						/*strdata[8] = BookMark[j];
						bookmarkmenu(i,j);*/
						
						/*if(i == 1){
							addCommand(command[2]);
							//stat3 &= ~0x8000400;
							//ListBox = tBookMark;
							//data[10] = data[67];	data[11] = data[68];
						// else if(i == 2){
							//stat3 ^= 0x0000400;
							//stat3 &= ~0x8000400;
							//ListBox = tBookMark;
							///data[10] = data[67];	data[11] = data[68];
							//addCommand(command[0]);
							//addCommand(command[3]);
						} else {
							//stat3 ^= 0x0000400;
							stat3 &= ~0x8000400;
							ListBox = tBookMark;
							data[10] = data[67];	data[11] = data[68];
							addCommand(command[0]);
							addCommand(command[3]);
						}
						stat2 &= ~0x0004000;	//function解除
						//showBookMark(1);
						stat |= 0x1000000;	//画面更新*/
					}

				}
				
			}

		} else if(keyCode == KEY_STAR){
			if( (stat & 0x0004000) != 0 ){	//板リストにいるとき
				strdata[7] = "板番号:" + ((data[22] + data[23]) * 100 + data[10] + data[11]);//StrList[10][7] + StrList[10][19];
				stat2 |= 0x0001000;
			} else if((stat & 0x0000100) == 0/*リストボックスは非表示*/ /*(stat2 & 0x0000F0F) == 0*//*!=設定画面*/){
				if((stat & 0x40000) != 0/*レス読み中*/){
					if( (stat2 & 0x0000080) != 0 ){	//AA MODE
						stat2 ^= 0x0000080;
					} else {
						stat2 |= 0x0000080;	//AA MODE
					}
					makeRes();
					//stat |= 0x1000000;	//画面更新
				} else if((stat & 0x10000) != 0 ){	//ｽﾚ一覧の表示
					data[92]++;
					if(data[92] == 3){data[92] = 0;}
					makeTL();
				}
			} else {
				stat2 |= 0x0004000;
			}
			stat |= 0x1000000;	//画面更新
/*

*/
		} else if(keyCode == KEY_POUND){	//#
			//if((data[57] & 0x00000100) != 0){data[57] &= ~0x00000100;	ColScm = ColPreset[0];
			//} else {data[57] |= 0x00000100;	ColScm = ColPreset[1];}
			if(data[58] < ColPreset.length - 1){
				data[58]++;
			} else if(data[58] == ColPreset.length - 1){
				data[58] = 0;
			}
			ColScm = ColPreset[data[58]];
			stat |= 0x1000000;	//画面更新
		}else if(keyCode == KEY_NUM0){ //0キーショートカット
			orezimenu(data[74]);
		}
		thttpget();	//httpgetdata()を新規スレッドで動作
		

	}
	/**
	 * レス指定用ボックスの呼び出しおよびレス指定処理の実行
	 * @param i 主に現在のレス番号を指定する初期値
	 * @param j 初期値(前半)
	 */
	public final void setResnum(int i, int j){
		if((stat & 0x0000400) != 0){	//設定した後
			stat ^= 0x0000400;
			stat2 ^= 0x0000040;//0x0000100;	//数字
			stat2 ^= 0x0002000;
			zuzu[0] = data[28];
			zuzu[1] = data[29];
			if((stat2 & 0x0000800) != 0/*data[26] == 1*/){
				httpinit( 2, i, zuzu[0], zuzu[0], j/*nThread[data[4]]*/);
			} else if(data[29] == 0){
				httpinit( 2, i, zuzu[0], zuzu[0] + data[1] - 1, j/*nThread[data[4]]*/);
			} else {
				httpinit( 2, i, zuzu[0], zuzu[1], j/*nThread[data[4]]*/);
			}

		} else {
			//NumSet();
			removeCommand(command[9]);
			removeCommand(command[6]);
			stat |= 0x0000400;
			stat2 |= 0x0000040;	//レス番号指定モード
			//data[26] = 0;	//0:-有り 1:-無し
			if(i >= 1000000){
				data[26] = 9999;
			} else {
				data[26] = 1000;
			}
			stat2 &= ~0x0000800;	//0:-有り 1:-無し
			data[27] = 5;	//選択している桁
			if(zuzu[0] != 0){
				data[28] = zuzu[0];	//初期値(前半)
				data[29] = zuzu[1];	//初期値(後半)
			}else{
				data[28] = j;	//初期値(前半)
				data[29] = 0;	//初期値(後半)
			}
			strdata[2] = StrList[3][2];
		}
	}
	/**
	 * 通信処理。板一覧の取得以外の処理はhttpgetdata()へと飛ばす。
	 * @see MainCanvas#httpgetdata()
	 */

	public final synchronized void thttpget(){

		if( (stat & 0x0010) != 0 && (stat3 & 0x0000800) == 0){	//通信中
			stat3 |= 0x0000800;		//通信機能使用中
			Thread thread = new Thread() {
				public final void run() {
					if((stat3 & 0x0100000) != 0){	//板一覧の取得
						stat3 &= ~0x0100000;
						stat |= 0x0010;	//通信
						httpgetdata();
						if((stat2 & 0x0001000) == 0){	//エラーメッセージが出ていないとき
							try {
								brdarray = dlarray;
								//DataOutputStream sp = new DataOutputStream(Connector.openOutputStream("scratchpad:///0;pos=" + 21010));
								//sp.writeUTF(new String(canv.dlarray));
								//sp.close();
								RecordStore recordStore = RecordStore.openRecordStore("Brdlist", true);
								if(recordStore.getNumRecords() == 0){
									recordStore.addRecord(brdarray, 0, brdarray.length);
								} else {
									recordStore.setRecord( 1, brdarray, 0, brdarray.length);
								}
								recordStore.closeRecordStore();//閉じ
							} catch(Exception e){}
							brdinit();
							strdata[7] = "板一覧取得完了";
							stat2 |= 0x0001000;
							stat |= 0x1000000;	//画面更新
						}
					} else {
						httpgetdata();
					}
				}
			};
			thread.start();
		}
/*
		Thread thread = new Thread() {
			public final void run() {
				if((stat3 & 0x0100000) != 0){	//板一覧の取得
					stat3 &= ~0x0100000;
					//stat |= 0x0001000;	//板一覧取得中
					//data[78] = 3;	//一般のダウンロード
					data[78] = 0x00000008;	//一般のダウンロード
					stat |= 0x0010;	//通信
					httpgetdata();
					if((stat2 & 0x0001000) == 0){	//エラーメッセージが出ていないとき
						try {
							DataOutputStream sp = new DataOutputStream(Connector.openOutputStream("scratchpad:///0;pos=" + 21010));
							sp.writeUTF(new String(dlarray));
							sp.close();
						} catch(Exception e){}
						brdinit();
						strdata[7] = "板一覧取得完了";
						stat2 |= 0x0001000;
					}
					//stat &= ~0x0001000;	//板一覧取得中
				} else if((stat3 & 0x0200000) != 0){	//キーリピート
					stat3 &= ~0x0200000;
					int k = data[83];//key;

					//keyPressed(k);
					while((stat2 & 0x2000000) != 0 && (getKeypadState() & k) != 0){
						keyPressed(k);
						try{
							sleep(data[82]*3);
						}catch (Exception e){}
					}
					keyth = null;
					stat2 ^= 0x2000000;
					//stat2 &= ~0x4000000;
				} else if( (stat & 0x0010) != 0 && (stat3 & 0x0000800) == 0){	//通信中
					httpgetdata();
				}
			}
		};
		thread.start();
*/
	}
	//キーリリース
	/**
	 * キーリリース
	 */
	protected synchronized void keyReleased(int keyCode) {
		if( keyCode != 0 ){
			int action = getGameAction(keyCode);


			//if(((stat & 0x0300000) != 0 || (stat2 & 0x0C00000) != 0) && (action & (DOWN | KEY_NUM8 | UP | KEY_NUM2 | LEFT | KEY_NUM4 | RIGHT | KEY_NUM6)) != 0 && (stat3 & 0x0010000) == 0/*スクロール実績*/){
			//	Scroll();
			//}
			stat &= ~0x0300000;//上下スクロールOFF
			stat2 &= ~0x0C00000;//左右スクロールOFF
			//if(action == DOWN || keyCode == KEY_NUM8){//keydown
			//	stat &= ~0x0200000;//下スクロールOFF
			//} else if(action == UP || keyCode == KEY_NUM2){//keyup
			//	stat &= ~0x0100000;//上スクロールOFF
			//} else if(action == LEFT || keyCode == KEY_NUM4){
			//	stat2 &= ~0x0400000;//左スクロールOFF
			//} else if(action == RIGHT || keyCode == KEY_NUM6){
			//	stat2 &= ~0x0800000;//右スクロールOFF
			//} else
			if(action == FIRE || keyCode == KEY_NUM5){
				int i = data[10] + data[11];
				int j = data[67] + data[68];
				int k = 0;
				if((stat3 & 0x0000400) != 0){//ﾌﾞｯｸﾏｰｸメニュー
					if((stat3 & 0x8000000) != 0){//ブックマークの特別メニュー表示
						i += 7;
					} else {
						if(BookMarkData[j * 3 + 1] == 0) {	//スレ番号＝0　板が登録されているor空
							//if(BookMark[j].equals("")/*BookMark[j].getBytes().length == 0*/){	//空
								//if(i == 0){i = 5;}
								//else{i += 6;}
							//} else {	//板
								//if(i == 0){i = 2;} else if(i == 1){i = 3;} else {i = 4;}
								i += 4;
							//}
						}
					}
					if((stat4 & 0x0080000) != 0){
						i = data[71];
						if(i != 1 /*|| i != 2 || i != 3*/){
							stat4 ^= 0x0080000;
						}
					}

					strdata[8] = BookMark[j];
					bookmarkmenu(i,j);
					if(i == 1){
						addCommand(command[2]);
					} else {
						//stat3 ^= 0x0000400;
						stat3 &= ~0x8000400;
						ListBox = tBookMark;
						data[10] = data[67];	data[11] = data[68];
						addCommand(command[0]);
						addCommand(command[3]);
					}

					stat2 &= ~0x0004000;	//function解除
					stat |= 0x1000000;	//画面更新
				}
/*			} else if(keyCode == KEY_NUM7){
				System.gc();
				Runtime runtime = Runtime.getRuntime();
				//strdata[7] = null;
				StrList[15] = new String[3];
				StrList[15][0] = "ﾒﾓﾘ情報";
				StrList[15][1] = "free:" + runtime.freeMemory();
				StrList[15][2] = "total:" + runtime.totalMemory();
				stat2 |= 0x0001000;
				stat |= 0x1000000;	//画面更新
*/
			} else if(keyCode == KEY_NUM9){//パケット表示から先読みに変更
				//if( (stat & 0x50000) != 0 ){	//ﾚｽ見てる時orｽﾚ選択中の時
					//viewcost();
					stat3 |= 0x0000080;	//先読みをすることを示すフラグ
	
					stat3 |= 0x0000100;	//参照元を保存しない
					strdata[8] = strdata[9];
					httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,nCacheTo[nCacheIndex]/*data[8]*/ + 1,nCacheTo[nCacheIndex]/*data[8]*/ + data[1],nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/);

					stat |= 0x1000000;	//画面更新
				//}
			} else if(keyCode == KEY_NUM0){


			}
			//if((stat & 0x0000004) != 0){	//キーストップがかかっていたら
			//	return;
			//}
			if(keyCode == KEY_STAR){
				stat2 &= ~0x0004000;
				stat |= 0x1000000;	//画面更新
			}
		}
	}
	/**
	 * メインの描画処理。将来的にはここを分割してpaintTLとpaintResとpaintMenuに分けたい。
	 */
	public synchronized final void paint(Graphics g/*raphics*/) {
		try{

			stat3 |= 0x1000000;	//描画中
			int i, j, k, byo = 0;
			int sc = data[77];	//スクロールの値のテンポラリ(文字がダブルの防止)
			int _stat;	//ステータス（コピー）
			String boxstr[];
			//stat |= 0x1000000;
//			System.out.println("g");
/*
			if( (stat2 & 0x0000040) != 0){
				return;
			}
*/
			if((data[49] & 0x02) != 0){//秒表示
				byo = 2;
			}

			if((data[57] & 0x00000008) != 0){
				if(byo == 0){byo = 5;} else {byo += 6;}
			}
			_stat = stat;
			g.setFont(font);
			if( (_stat & 0xF000000) != 0 ){//1+2+4+8
				stat &= ~0xF000000;	//再描画フラグ消去

//				stat2 |= 0x0000040;//描画ロック
				//Graphics g = getGraphics();
				//g.lock();
				if( (_stat & 0xD000000) != 0 ){//13=1+4+8
					if( (_stat & 0x1000000) != 0 ){	//全体を再描画
						g.setColor(ColScm[0],ColScm[1],ColScm[2]);//2chのスレ背景色 239,239,239
						g.fillRect(0, data[30] + 3, width, height);

						if( (_stat & 0x0450000) != 0){// || (stat & 0x40000) != 0 ){	//ｽﾚ一覧の表示 or レスの表示
//							g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//0,0,0
//							if( (_stat & 0x10000) != 0 ){	//ｽﾚ一覧の表示
								//if(nRes[data[5] % data[0]] >= 1000){i = 3;}else if(nRes[data[5] % data[0]] >= 100){i = 2;}else if(nRes[data[5] % data[0]] >= 10){i = 1;}else {i = 0;}
//								i = GetDigits(nCacheBrdData[nCacheIndex][data[4]]/*nRes[data[4]]*/);

//								g.drawString(nCacheBrdData[nCacheIndex][data[4]]/*nRes[data[4]]*/ + "ﾚｽ", width - 6 - (i + 2) * data[33], data[12] - 1 - data[32], 68/*g.BASELINE|g.LEFT*/);
//								i = data[5] - data[4] + 1;
//								g.drawString("ｽﾚ" + (data[5] + 1) + "/" + i + "-" + (i + nCacheTh[nCacheIndex]/*nThread*/.length - 1), 6, data[12] - 1 - data[32], 68/*g.BASELINE|g.LEFT*/);
								//g.setColor(0,0,255);
					//		} else if( (_stat & 0x0400000) != 0 ){	//データフォルダの表示
					//			g.drawString("test" + (sc / (data[30] + data[34]))  + " " + DivStr.length + " " + DivStr[0], 0, 40, 20/*g.TOP|g.LEFT*/);//名前
//							}
							//レスの表示
							j = data[30] + data[34];//高さ+行間
							i = sc / j;	if(0 > i){i = 0;}	//DivStrの書き始め
							k = (height + sc - data[30] - 4) / j + 1;	//これ以上は表示されないので書かない
							if(k > data[85]){k = data[85];}	//indexからはずれないために
							//リンクの所を色塗り
							//if( (_stat & 0x40000) != 0 ){	//レスの表示
							int n, m, o, p;//, q;
//							data[60] = -1;
//							q = data[65];
							for(n = 0;n < data[59];n++){
								m = (Linklist[n] / 1000) % 10000;
								if(m >= i || m <= k){	//リンク張るところは表示内にある場合
									//System.out.println("m:" + m + " i:" + i + " sc:" + sc + " 62:" + data[60] + " n:" + n + " q:" + q);
//									if(data[60] == -1 && ((sc == -data[30] && m == 0)/*一行目*/ || (m - i) > 0/*隠れている場合はフォーカスが当たらない*/)){
//										if(q == 0){	//skip分がない場合
//											//System.out.println("focus:" + n);
//											g.setColor(ColScm[24],ColScm[25],ColScm[26]);	//リンクの色(focus)
//											data[60] = n - data[65];
//										} else {	//ある場合
//											q--;
//											g.setColor(ColScm[27],ColScm[28],ColScm[29]);	//リンクの色
//										}
									if((data[57] & 0x00004000) != 0 && nCacheInfo[n] == 1){
										if(data[60] == n){
											g.setColor(ColScm[30],ColScm[31],ColScm[32]);	//リンクの色(focus,cached)
										} else {
											g.setColor(ColScm[33],ColScm[34],ColScm[35]);	//リンクの色(cached)
										}
									} else {
										if(data[60] == n){
											g.setColor(ColScm[24],ColScm[25],ColScm[26]);	//リンクの色(focus)
										} else {
											g.setColor(ColScm[27],ColScm[28],ColScm[29]);	//リンクの色
										}
									}
									o = (Linklist[n] / 1000) / 10000;
									if((stat2 & 0x0000080) == 0){	//AA MODEではない場合
										//System.out.println("linklist:" + Linklist[n] + " n:" + n);
										g.fillRect(data[48] + Linklist[n] % 1000 * data[33], 4 + (m + 1/*この1は1行目用*/) * j - sc, Math.min(o, data[42] - Linklist[n] % 1000) * data[33], data[30]);
										o -= data[42] - Linklist[n] % 1000;
										for(p = 1;o > 0;p++){
											g.fillRect(data[48], 4 + (m + 1/*この1は1行目用*/ + p) * j - sc, Math.min(o, data[42]) * data[33], data[30]);
											o -= data[42];
										}
									} else{
										g.fillRect(data[48] + Linklist[n] % 1000 * data[33], 4 + (m + 1/*この1は1行目用*/) * j - sc, o * data[33], data[30]);
									}
								}
							}
							if( 1 > sc && (_stat & 0x40000) != 0 ){	//１行目の名前欄を表示する場合
								//i = GetDigits(data[6] + data[7]) - 1;

								g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//文字の色 0,0,0
								g.drawString((data[6] + nCacheSt[nCacheIndex]/*data[7]*/) + ":", data[48], 4 - sc, 20/*g.TOP|g.LEFT*/);
								g.setColor(ColScm[18],ColScm[19],ColScm[20]);//ﾚｽの名前 green
								g.drawString(strdata[3]/*ResElements[0]*/, (GetDigits(data[6] + nCacheSt[nCacheIndex]/*data[7]*/) + 1) * data[33] + data[48], 4 - sc, 20/*g.TOP|g.LEFT*/);//名前
							}
							//}

							g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//文字の色 0,0,0
							sc -= 4 + j;	//トップの余白1+ライン2+余白1+1行目用
							if( (_stat & 0x0040000) != 0){	//レスの表示
								if(i == 0){
									g.drawSubstring(resstr, 0, iDivStr[0], data[48], i * j - sc, 20);	i++;
								}
								for (; i < k; i++) {
									g.drawSubstring(resstr, iDivStr[i-1], iDivStr[i] - iDivStr[i-1], data[48], i * j - sc, 20);
								}
							} else {
								for (; i < k; i++) {
									//if(16 + i * j - sc > height){break;}
									g.drawString(DivStr[i], data[48], i * j - sc, 20/*g.TOP|g.LEFT*/);
								}
							}
							sc += 4 + j;	//トップの余白1+ライン2+余白1+1行目用
						}
					}
					if( (_stat & 0x5000000) != 0 && (_stat & 0x0100) != 0 ){		//List Box
						g.setColor(ColScm[0],ColScm[1],ColScm[2]);//2chのスレ背景色 239,239,239
						g.fillRect(8, data[12] + 2, width - 17, data[13] - 6);
	/*						if( (_stat & 0x4000000) != 0 ){		//List Box再描画
							g.fillRect(10, data[12] + 2, width - 20, data[13] - 7);
						} else {
							g.fillRect(10, data[12] + 2, width - 21, data[13] - 8);
						}
	*/
						if((stat2 & 0x0004000) != 0){
							g.setColor(ColScm[15],ColScm[16],ColScm[17]);	//ﾘｽﾄ選択色(Function) 255,165,0
						} else {
							g.setColor(ColScm[12],ColScm[13],ColScm[14]);	//ﾘｽﾄ選択色 192,192,255
						}
						g.fillRect(12, data[12] + 7 + data[11] * (data[30] + 3), width - 26, data[30]);	//選択表示
						if(wallpaper != null && data[12] == data[30] + 3 && (data[57] & 0x00000400) == 0){
							//g.drawImage(wallpaper,9 + data[37],data[12] + data[38], 20/*g.TOP|g.LEFT*/ );//モナーの絵
							g.drawImage(wallpaper,data[37],/*data[12] + */data[38], 20/*g.TOP|g.LEFT*/ );//モナーの絵
						}
						g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//文字の色 0,0,0
						j = (data[13] - 13) / (data[30] + 3);
						for (i = 0; i < j; i++) {
							if(i + data[10] >= ListBox.length){break;}
							if(ListBox[i + data[10]] != null){
								g.drawString(ListBox[i + data[10]], 12, data[12] + 7 + i * (data[30] + 3), 20/*g.TOP|g.LEFT*/);
							}
							if(/*data[12] + */5 + (i + 1) * (data[30] + 3) < /*data[12] + */data[13] - 8){	//区切り線を引く
								g.setColor(ColScm[9],ColScm[10],ColScm[11]);	//中間の濃さ 128,128,128
								g.drawLine(12, data[12] + 5 + (i + 1) * (data[30] + 3), width - 15, data[12] + 5 + (i + 1) * (data[30] + 3));
								g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//文字の色 0,0,0
							}
						}
						g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//文字の色 0,0,0
						if(ListBox.length > (data[13] -13)  / (data[30] + 3) + data[10]){	//下矢印
							DrawTriangle(g, width / 2, data[12] + data[13] - 8, 0);
						}
						if(data[10] > 0){	//上矢印
							DrawTriangle(g, width / 2, data[12] + 1, 1);
						}
						//if( (_stat & 0x4000000) == 0 ){		//List Boxの再描画でない場合(全体を再描画)
						if( (_stat & 0x1000000) != 0 ){		//全体を再描画
							//枠
							g.setColor(ColScm[9],ColScm[10],ColScm[11]);	//中間の濃さ 128,128,128
							g.drawLine(8, data[12] + 1, width - 10, data[12] + 1);//上
							g.drawLine(8, data[12] + data[13] - 4, width - 10, data[12] + data[13] - 4);//下
							g.drawLine(7, data[12] + 2, 7, data[12] + data[13] - 5);//左
							g.drawLine(width - 9, data[12] + 2, width - 9, data[12] + data[13] - 4/*- 5*/);//右
							g.setColor(ColScm[6],ColScm[7],ColScm[8]);	//薄い色 192,192,192
							//g.drawLine(width - 8, 18, width - 8, height - 4);
							g.fillRect(width - 8, data[12] + 3, 3, data[13] - 4);
							g.fillRect(10, data[12] + data[13] - 3, width - 18, 2);
						}
					}
					if( (_stat & 0x9000000) != 0 ){
						if((_stat & 0x0000400) != 0 && (stat2 & 0x0000340) != 0){	//設定ダイアログ && 数字or文字モード
							k = 0;
							if((stat2 & 0x1001000) != 0){	//文字列表示orインフォメーション表示	0x0001000 || 0x1000000
								k = data[30] * -2;
							}
							if((stat2 & 0x0000040) != 0){//レス番号指定モード
								i = GetDigits(data[28]);
								strdata[6] = "";
								for(j = 0;j < 4 - i;j++){
									strdata[6] = strdata[6] + "0";
								}
								String str = strdata[2] + ":" + strdata[6] + data[28];
								//if(data[26] == 0){
								if((stat2 & 0x0000800) == 0){
									i = GetDigits(data[29]);

									strdata[6] = "";
									for(j = 0;j < 4 - i;j++){
										strdata[6] = strdata[6] + "0";
									}
									str += "-" + strdata[6] + data[29];
								} else {
									str += "     ";
								}

								i = DrawBoxStr( g, data[50], data[51]+k, null, str, 4) + data[33] / 2;
							} else if((stat2 & 0x0000100) != 0){//数字
//									if(data[26] >= 1000){i = 4;}else if(data[26] >= 100){i = 3;}else if(data[26] >= 10){i = 2;}else {i = 1;}
								//if(data[28] >= 1000){i = 4;}else if(data[28] >= 100){i = 3;}else if(data[28] >= 10){i = 2;}else {i = 1;}
								i = GetDigits(data[28]);
								strdata[6] = "";
								for(j = 0;j < data[29] + 1 - i;j++){
									strdata[6] = strdata[6] + "0";
								}
								//i = DrawLineStr( g, data[50], data[51], strdata[2] + ":" + strdata[6] + "" + data[28], 4) + data[33] / 2;
								i = DrawBoxStr( g, data[50], data[51]+k, null, strdata[2] + ":" + strdata[6] + data[28], 4) + data[33] / 2;
							} else {//一文字
								//i = DrawLineStr( g, data[50], data[51], strdata[2] + ":" + StrList[14][data[28]], 4);
								i = DrawBoxStr( g, data[50], data[51]+k, null, strdata[2] + ":" + StrList[14][data[28]], 4);
								/*
								if(data[28] < data[26]){
									DrawTriangle(g, i - data[33] - 1, height / 2 - data[33] - 7, 1);//ue
								}
								if(data[28] > 0){
									DrawTriangle(g, i - data[33] - 1, height / 2 + data[33], 0);//sita
								}*/
							}
							if((stat2 & 0x0000040) == 0 || data[27] > 4){
								j = data[28];
							} else if(data[27] < 4) {
								j = data[29];
							} else {
								j = -1;
							}
							if(j < data[26] || ((stat2 & 0x0000040) != 0 && j < 1000) || j == -1){
								DrawTriangle(g, i - data[33] * (data[27] + 1)/* - 1*/, data[51] - data[33] - 7 + k, 1);//ue
							}
							if(j > 1 || ((stat2 & 0x0002000) == 0 && data[28] > 0) || j == -1){
								DrawTriangle(g, i - data[33] * (data[27] + 1)/* - 1*/, data[51] + data[33] + k, 0);//sita
							}
//							} else if((stat2 & 0x0000400) != 0){	//色設定モード
//								DrawLineStr( g, width / 2, height / 2 - (data[30] + 10), strdata[2], 0);
//								DrawLineStr( g, width / 2, height / 2, "R:   G:   B:", 3);
						}
					}
					//stat &= ~0x1000000;//再描画OFF
					//if( (stat & 0x1000000) != 0 ){	//再描画OFF
					//	stat ^= 0x1000000;
					//}
				}
				if( (_stat & 0x5000000) != 0 || (_stat & 0x2000000) == 0){	//トップしか更新しないときは描画しない。
					if(data[56] > 1 && (_stat & 0x0000100) == 0 && (_stat & 0x0010000) != 0 && nCacheTh[nCacheIndex][data[60]] > 900000000){	//スレ選択中
						calendar.setTime(new Date((long)nCacheTh[nCacheIndex][data[60]] * 1000));
						if(data[92] == 1){
							boxstr = new String[3];
							boxstr[0] = "ﾚｽ:" + nCacheBrdData[nCacheIndex][data[60]];
							i = height - (data[30] + data[34]) * 3 / 2 - 5;
							j = 1;
						} else {
							boxstr = new String[2];
							i = height - (data[30] + data[34]) - 5;
							j = 0;
						}
						boxstr[j] = "since:" + (calendar.get(1/*Calendar.YEAR*/)) + "/" + (calendar.get(2/*Calendar.MONTH*/)+1) + "/" + calendar.get(5/*Calendar.DATE*/);
						boxstr[j+1] = "勢い:" + nCacheBrdData[nCacheIndex][data[60]] * 86400 /(System.currentTimeMillis()/ 1000 - nCacheTh[nCacheIndex][data[60]]) + "ﾚｽ/日";
						//if(data[60] > nCacheTh[nCacheIndex].length / 2){
						//if(((Linklist[data[60]] / 1000) % 10000 + 1/*この1は1行目用*/) * (data[30] + data[34]) - sc > height / 2){
						//	i = data[30] + (data[30] + data[34]) + 7;
						//} else {
						//}
						DrawBoxStr( g, width - data[33] * 9, i, boxstr, null, 0);
					}
					if((stat2 & 0x1001000) != 0){	//文字列表示orインフォメーション表示	0x0001000 || 0x1000000
						//if(strdata[7] != null){
						//	DrawLineStr( g, data[50], data[51], strdata[7], 0);
						//} else {
						//	DrawBoxStr( g, data[52], data[53], StrList[15]);
						//}
						k = 0;
						if((_stat & 0x0000400) != 0 && (stat2 & 0x0000340) != 0){	//設定ダイアログ && 数字or文字モード
							k = data[30] + 5;
						}
						DrawBoxStr( g, data[52], data[53]+k, StrList[15], strdata[7], 0);
					} else if((_stat & 0x0000004) != 0){	//キーストップがかかっていたら
						if((_stat & 0x0020000) != 0){
							i = width - data[33] * 6 + 3; j = height - data[33] - 5;
							//DrawLineStr( g, width - data[33] * 6 + 3, height - data[33] - 5, "Loading…"/*StrList[10][10]*/, 0);
						} else {
							i = width / 2; j = height / 2;
							//DrawLineStr( g, width / 2, height / 2, "Loading…"/*StrList[10][10]*/, 0);
						}
						DrawBoxStr( g, i, j, null, "Loading…"/*StrList[10][10]*/, 0);
					}
				}
				if((_stat & 0x0800000) == 0 || (_stat & 0x1000000) != 0){	//トップの再描画は必要ないことはない場合
					//トップ
					g.setColor(ColScm[0],ColScm[1],ColScm[2]);//2chのスレ背景色 239,239,239
					g.fillRect(0, 0, width, data[30] + 2);
					//g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//文字の色 0,0,0
					//data[39] = data[31]/* - 1*/;
					if( (_stat & 0x00DF010) == 0 && (stat2 & 0x001000F) == 0/* && (stat2 & 0x0000020) != 0*/){	//タイトル画面の場合
						g.setColor(0, 0, 255);	//blue
						g.drawString( "iMona@zuzu "/* + StrList[10][0]*/ + "v" + version, 0, 1, 20/*g.TOP|g.LEFT*/);

					}
					g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//文字の色 0,0,0

					/*if( (stat & 0x00E0) == 0x0080 ){	//通信エラー
						g.drawString("\ue6d1", 30, data[31]);//iマーク
						g.drawString("×", 30, data[31]);//×マーク
					} else */
	//				if( (stat & 0x10000) != 0 || (stat & 0x40000) != 0 ){	//ｽﾚ一覧の表示 or レスの表示
					k = width - data[33] * byo - 4;
					if((_stat & 0x0000010) == 0/*通信中ではない*/ || byo == 0/*秒表示*/){
						if(byo == 0){
							k += 2;
						}
						if( (_stat & 0x50000) != 0 ){	//ｽﾚ一覧の表示 or レスの表示
							if((_stat & 0x0000010) == 0 && (stat & 0x10000000) == 0){
								//スレ名・板名の表示
								if(strdata[9] != null){
									g.drawString(strdata[9], 1, 1, 20/*g.TOP|g.LEFT*/);//板・スレの名前
								}
								/*
								if( (_stat & 0x40000) != 0 ){//レスの表示
									if(ThreadName[data[4]] != null){
										g.drawString(ThreadName[data[4]], 1, data[31]+1, g.BASELINE|g.LEFT);//スレッド名
									}
								} else {
									if(strdata[9] != null){
										g.drawString(strdata[9], 1, data[31]+1, g.BASELINE|g.LEFT);//板の名前
									}
								}
								*/
								g.setColor(ColScm[0],ColScm[1],ColScm[2]);//2chのスレ背景色 239,239,239
								g.fillRect(k - data[33] * 8 - 4, 0, data[33] * (8+byo) + 9, data[30]);
								g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//文字の色 0,0,0
							}
							if( (_stat & 0x10000) != 0 ){	//ｽﾚ一覧の表示
								//i = data[4];
								//j = CacheBrdData[nCacheIndex]/*ThreadName*/.length - 1;
								i = j = -1;
							} else if( (_stat & 0x40000) != 0 ){	//レスの表示
								i = data[6];
								j = nCacheTo[nCacheIndex]/*data[8]*/ - nCacheSt[nCacheIndex]/*data[7]*/;
							} else {i = 0;j = 0;}
							if( (stat2 & 0x0000080) != 0 ){	//AA MODE
								k += - data[33] * 8 + 7;
								g.setColor(ColScm[21],ColScm[22],ColScm[23]);	//注意色
								g.drawString("AA MODE", k, 1, 20/*g.TOP|g.LEFT*/);
								//g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//文字の色 0,0,0
							} else {
								k += - data[33] * 4;
								if(i < j){g.drawString("次≫", k, 1, 20/*g.TOP|g.LEFT*/);}
								else {
									if((stat3 & 0x0800000) != 0){
										g.drawString("蓄≫", k, 1, 20/*g.TOP|g.LEFT*/);
									} else {
										g.setColor(ColScm[21],ColScm[22],ColScm[23]);	//注意色
										g.drawString("接≫", k, 1, 20/*g.TOP|g.LEFT*/);
										g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//文字の色 0,0,0
									}
								}
								k += - data[33] * 4 - 3;
								if(i > 0){g.drawString("≪前", k, 1, 20/*g.TOP|g.LEFT*/);
								//} else if( ((_stat & 0x40000) != 0) && nCacheSt[nCacheIndex]/*data[7]*/ == 1){
								//	g.drawString("≪ス", k, data[31]+1, 68/*g.BASELINE|g.LEFT*/);
								//} else if( ((_stat & 0x10000) != 0) && nCacheSt[nCacheIndex] == 1/*data[5] == 0*/){
								//	g.drawString("≪板", k, data[31]+1, 68/*g.BASELINE|g.LEFT*/);
								} else if( ((_stat & 0x50000) != 0) && nCacheSt[nCacheIndex] == 1){
									g.drawString("≪板", k, 1, 20/*g.TOP|g.LEFT*/);
								} else {
									if((stat3 & 0x0400000) != 0){
										g.drawString("≪蓄", k, 1, 20/*g.TOP|g.LEFT*/);
									} else {
										g.setColor(ColScm[21],ColScm[22],ColScm[23]);	//注意色
										g.drawString("≪接", k, 1, 20/*g.TOP|g.LEFT*/);
										//g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//文字の色 0,0,0
									}
								}
							}
							g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//文字の色 0,0,0
							if((_stat & 0x0000100) == 0 && (_stat & 0x0450000) != 0  && (data[57] & 0x00004000) != 0 && data[60] >= 0 && nCacheInfo[data[60]] == 1){
								g.setColor(ColScm[0],ColScm[1],ColScm[2]);//2chのスレ背景色 239,239,239
								g.fillRect(k - data[33]*3, 0, data[33]*3, data[30]);
								g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//文字の色 0,0,0
								g.drawString("[C]", k - data[33]*3, 1, 20/*g.TOP|g.LEFT*/);
								k -= data[33]*3;
							//	DrawBoxStr( g,  data[33] * 4 + 2, height - data[33] - 5, null, "ｷｬｯｼｭ有"/*StrList[10][10]*/, 0);
							}
							//g.drawString((i + 1) + "/" + (j + 1), 30, data[31]);
							//g.drawString((data[6] + data[7]) + "/" + data[9], 30, data[31]);
						} else if( (_stat & 0x0002000) != 0 || (_stat & 0x0004000) != 0 || (stat2 & 0x001000F) != 0/* || (stat3 & 0x0000400) != 0 */){	//カテゴリ選択中 or 板選択中 or 設定 or ブックマーク(メニュー)の表示中
							//k = width - data[33] * (8+byo) - 3 - byo;
		//				} else if( (stat & 0x100000) != 0 ){	//通信表示
		//					g.drawString("\ue6d1", 0, data[31]);//iマーク
							if((_stat & 0x0000010) == 0){	//通信中ではないとき
								if((stat3 & 0x0000400) != 0){	//ブックマーク(メニュー)の表示中
									g.drawString( BookMark[data[67]+data[68]], 0, 1, 20/*g.TOP|g.LEFT*/);
								} else {
									g.setColor(ColScm[21],ColScm[22],ColScm[23]);	//注意色
									if( (_stat & 0x0002000) != 0){
										g.drawString( "ｶﾃｺﾞﾘ", 0, 1, 20/*g.TOP|g.LEFT*/);
									} else if( (_stat & 0x0004000) != 0 ){
										g.drawString( "板 - " + StrList[12][data[22]+data[23]], 0, 1, 20/*g.TOP|g.LEFT*/);
									} else if((stat2 & 0x0010000) != 0 ){	//ブックマーク
										g.drawString( "ﾌﾞｯｸﾏｰｸ", 0, 1, 20/*g.TOP|g.LEFT*/);
									} else if((stat2 & 0x0000001) != 0){	//設定の時
										g.drawString( "設定", 0, 1, 20/*g.TOP|g.LEFT*/);
									}
								}
							}
							g.setColor(ColScm[0],ColScm[1],ColScm[2]);//2chのスレ背景色 239,239,239
							if( (stat2 & 0x0010000) != 0 && (stat3 & 0x0000400) == 0 ){	//ブックマークの表示中
								k += - data[33] * 9;
								g.fillRect(k, 0, data[33] * 9 + 2, data[30]);
								g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//文字の色 0,0,0
								g.drawString("≪前 Me≫", k, 1, 20/*g.TOP|g.LEFT*/);
							} else {
								k += - data[33] * 4;
								g.fillRect(k, 0, data[33] * 4 + 2, data[30]);
								g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//文字の色 0,0,0
								g.drawString("≪前", k, 1, 20/*g.TOP|g.LEFT*/);
							}
						}
					}
					if( (stat & 0x10000000) != 0 || (_stat & 0x0000010) != 0 ){	//ダウンロードの進行表示バー
						g.setColor(ColScm[6],ColScm[7],ColScm[8]);	//薄い色 192,192,192
						g.drawRect(0, 0, k-1, data[30] - 1);
						if(data[16] != 0 && data[18] != 0){
							g.setColor(ColScm[12],ColScm[13],ColScm[14]);	//ﾘｽﾄ選択色 192,192,255
							g.fillRect(1, 1, (k-2) * data[16] / data[18], data[30] - 2);
						}
						g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//文字の色 0,0,0
						if(data[16] == 0){
							//if(StrList[10] != null)
								g.drawString("接続中"/*StrList[10][8]*/, 1, 0, 20/*g.TOP|g.LEFT*/);//接続中
						} else if(data[16] == data[18]){
							//if(StrList[10] != null)
								g.drawString("処理中"/*StrList[10][9]*/, 1, 0, 20/*g.TOP|g.LEFT*/);//処理中
						} else if(data[18] != 0){
							//datasize = "" + boardsize;
							g.drawString(data[16] + "/" + data[18], 1, 0, 20/*g.TOP|g.LEFT*/);//通信中
						}
					//} else if( (_stat & 0x50000) != 0 ){	//ｽﾚ一覧の表示 or レスの表示
					}
					//区切りバー
					g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//文字の色 0,0,0
					g.drawLine(0,data[30] + 3 - 1,width-1,data[30] + 3 - 1);
					g.setColor(ColScm[9],ColScm[10],ColScm[11]);	//中間の濃さ 128,128,128
					g.drawLine(0,data[30] + 3 - 2,width-1,data[30] + 3 - 2);
					//トップの描画終わり
				} else {	//トップの再描画は必要ない
					stat &= ~0x0800000;	//トップの再描画は必要ないを解除
				}
	//				g.drawString("m:" + Runtime.getRuntime().freeMemory() + "/" + Runtime.getRuntime().totalMemory(), 0, 50);
				//g.unlock(true);
				//stat &= ~0xF000000;	//再描画フラグ消去
				//if( (stat & 0x2000000) != 0 ){
				//	stat ^= 0x2000000;
				//}
////				graphics.drawImage( img2, 0, 0, 20/*g.TOP|g.LEFT*/);	//画面に転送
//				stat2 ^= 0x0000040;//描画ロック解除
			}

			if(byo != 0){//秒表示
				calendar.setTime(new Date(System.currentTimeMillis()));

				//塗りつぶし
				g.setColor(ColScm[0],ColScm[1],ColScm[2]);//2chのスレ背景色 239,239,239
				g.fillRect(width - data[33] * byo - 2, 0, data[33] * byo + 2, data[30] + 1);

				if( (_stat & 0x7000000) != 0 ){
					//区切り線
					g.setColor(ColScm[9],ColScm[10],ColScm[11]);	//中間の濃さ 128,128,128
					g.drawLine( width - data[33] * byo - 3, 0, width - data[33] * byo - 3, data[30]);
				}

				g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//文字の色 0,0,0
				if(byo == 2 || byo >= 7){
					//秒の表示
					g.drawString((calendar.get(Calendar.SECOND) / 10) + "" + (calendar.get(Calendar.SECOND) % 10) + "", width-1, 1, 24/*g.TOP|g.RIGHT*/);
					if(byo == 8){
						g.drawString(":", width-1-data[33] * 2, 1, 24/*g.TOP|g.RIGHT*/);
					}
				}

				if(byo >= 5){
					//時・分の表示
					//if(calendar.get(Calendar.HOUR_OF_DAY) < 10){byo--;}
					g.drawString(calendar.get(Calendar.HOUR_OF_DAY) + ":" + (calendar.get(Calendar.MINUTE) / 10) + "" + (calendar.get(Calendar.MINUTE) % 10), width-1-data[33] * (byo - 5), 1, 24/*g.TOP|g.RIGHT*/);
				}
			}



			if (data[91] > 0 && data[91] < data[47] / 100){	//パケット代が設定された金額を超えている場合
				//g.setColor(ColScm[21],ColScm[22],ColScm[23]);	//注意色
				DrawBoxStr( g, data[52], height - 10 - data[30], null, "ﾊﾟｹｯﾄ代警告!!(\\" + (data[47] / 100) + ")", 0);
			}
			if(data[95] == -1){
				DeviceControl dc = DeviceControl.getDefaultDeviceControl();
				DrawBoxStr( g, data[52], height - 10 - data[30]*3, null, "\uE00A" + dc.getDeviceState(dc.BATTERY) + "% \uE20B" + dc.getDeviceState(dc.FIELD_INTENSITY) + "%", 0);
			}
		} catch(Exception e){
		}
		stat3 &= ~0x1000000;	//描画中解除
	}

	/**
	 * Stringのトリミング処理。主にブックマーク系から呼ばれる。
	 * @param str トリミングしたい文字列
	 * @param w トリミングしたい最大数値
	 * @return String
	 */
	public final String trimstr(final String str, int w){
		if(str.getBytes().length > w / data[33]){
			byte[] b = str.getBytes();
			int n = 0;
			w = w / data[33] - 1;
			for(int i = 0; i < b.length; i++){
				if(0 > b[i] && (b[i] <= -97 || -32 <= b[i])){//SJISの１バイト目(高速モード(判定ミスが起こるかも))
					i++;
				}
				if(i >= w){break;}
				n++;
			}
			return str.substring(0, n) + "..";
			//data[42] = w / data[33] - 1;
			//String[] stmp = FastSeparateByte(str.getBytes());
			//data[42] = width / data[33];	//一行の文字(byte)数
			//return stmp[0] + "..";
		}

		return str;
	}
	//mode == 0 : 通常動作
	//mode > 1  : リンクのキャッシュチェック(リンクの箇所はmode - 1)

	/**
	 * リンクを開くための処理
	 * @param url リンクで開きたいアドレスを指定
	 * @param mode 0の場合は通常動作、1よりも大きい場合はリンクのキャッシュチェック(リンクの箇所はmode - 1)
	 */
	public final void Link(String url, int mode){
		int i, j;
		if(url == null){
			int link = data[60]/* + data[65]*/;
			if(link == -1){	//link
				return;
			}
			if(mode > 0){
				link = mode -1;
			}
			int n, to = -1;//, m = Linklist[link] / 1000;

//			j = m / 10000;	//リンクの文字数
			//byte[] b = DivStr[(Linklist[link] / 1000) % 10000].getBytes();
			url = Linkurllist[link];
			byte[] b = url.getBytes();
			if(b[0] == 62 || b[0] == 129){	//レス内リンク
				j = 1;	n = 0;
				for(i = b.length - 1/*(Linklist[link] / 1000) / 10000 - 1*/;i > 0;i--){
					if(b[i] != 62 && b[i] != 132){
						if(b[i] == 45){	//-
							to = n;	n = 0;	j = 1;
						} else {
							n += (b[i] - 48) * j;
							j = j * 10;
						}
					}
				}
				//from = n;
				if(n > 0){
					if(to == -1){
						to = n;//from;// + data[1] - 1;
					} else if(to > n/*from*/ + data[1] - 1){
						to = n/*from*/ + data[1] - 1;
					}

					/*
					if(data[7] <= from && from <= data[8]){
						data[6] = from - data[7];
						makeRes();
						stat |= 0x1000000;	//画面更新
					} else {
						httpinit(2,data[3],from,to,nThread[data[4]]);
					}*/
					strdata[8] = strdata[9];
					if(mode > 0){
						httpinit( 12, nCacheBrd[nCacheIndex]/*data[3]*/, n/*from*/, to, nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/);
					} else {
						httpinit( 2, nCacheBrd[nCacheIndex]/*data[3]*/, n/*from*/, to, nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/);
					}
				}
				return;
			}
/*			 else {	//URL
				Link(url);
/*
				j = url.indexOf(".") + 1;
				//System.out.println("<" + url + ">" + url.indexOf("2ch.net", k + 1) + " " + k + " " + url.indexOf("bbspink.com", k + 1));
				if(url.indexOf("2ch.net", j) == j || url.indexOf("bbspink.com", j) == j){
					strdata[6] = url;
					httpinit(5, 0, 1, 0, 0);
				} else {
					url = url.substring(7);
					//}
					openbrowser(url);
				}
*/
		}
		if(mode > 0){return;}

		j = url.indexOf(".") + 1;
		//System.out.println("<" + url + ">" + url.indexOf("2ch.net", k + 1) + " " + k + " " + url.indexOf("bbspink.com", k + 1));
		//URLジャンプ

		if(url.indexOf("www.2ch.net", j) == j || url.indexOf("www-2ch.net", j) == j || url.indexOf("2ch.net/ad.htm", j) == j || url.indexOf("mup.vip2ch.com") != -1 || url.indexOf("wktk.vip2ch.com") != -1 || url.indexOf("c-docomo.2ch.net") != -1){
			url = url.substring(7);
			//}
			openbrowser(url,0);
		}else if(url.indexOf("jbbs.livedoor.jp") != -1){ //したらばのURL読込
			int br, sp, ep, w;
			String p , s, t , d;
			s = "0";
			t = "auto";
			if(url.indexOf("auto") != -1){
				t = "auto";
				s = "10";
			}else if(url.indexOf("computer") != -1){
				t = "computer";
				s = "11";
			}else if(url.indexOf("game") != -1){
				t = "game";
				s = "12";
			}else if(url.indexOf("movie") != -1){
				t = "movie";
				s = "13";
			}else if(url.indexOf("music") != -1){
				t = "music";
				s = "14";
			}else if(url.indexOf("shop") != -1){
				t = "shop";
				s = "15";
			}else if(url.indexOf("sports") != -1){
				t = "sports";
				s = "16";
			}else if(url.indexOf("travel") != -1){
				t = "travel";
				s = "17";
			}else if(url.indexOf("ness") != -1){
				t = "ness";
				s = "18";
			}else if(url.indexOf("study") != -1){
				t = "study";
				s = "19";
			}else if(url.indexOf("news") != -1){
				t = "news";
				s = "20";
			}else if(url.indexOf("otaku") != -1){
				t = "otaku";
				s = "21";
			}else if(url.indexOf("anime") != -1){
				t = "anime";
				s = "22";
			}else if(url.indexOf("comic") != -1){
				t = "comic";
				s = "23";
			}else if(url.indexOf("school") != -1){
				t = "school";
				s = "24";
			}
			//d = "jbbs.livedoor.jp/" + t + "/";
			//sp = url.indexOf(d);
			//sp = sp + d.length();
			sp = url.lastIndexOf('/') + 1;
			ep = url.length();
			p = url.substring( sp, ep);
			w = p.length();
			for (i = w; i < 5; i++) {
			    p = "0" + p;
			}
			//System.out.println(t);
			br = Integer.parseInt(s + p);
			//br.parseInt(s + p);//s + p
			stat2 = 0x0010000;
			httpinit(1, br, 1, data[0], 0);
			//url.indexOf()
			//url.substring();
			//t = s.;
			return;
		} else if(url.indexOf("2ch.net", j) == j || url.indexOf("bbspink.com", j) == j || url.indexOf("kakiko.com", j) == j || url.indexOf("vip2ch.com", j) == j || url.indexOf("jbbs.livedoor.jp") != -1){
			strdata[6] = url;
			//System.out.println(url);
			httpinit(5, 0, 1, 0, 0);
			//thttpget();	//httpgetdata()を新規スレッドで動作
		} else {
			url = url.substring(7);
			//}
			openbrowser(url,0);
		}
	}
	/**
	 * ブラウザ起動処理
	 * @param url ブラウザで開きたいアドレスを指定
	 * @param mode 0の場合は通常動作、1よりも大きい場合はリンクのキャッシュチェック(リンクの箇所はmode - 1)
	 */
	public final void openbrowser(String url, int mode){
		try {
			if( (stat4 & 0x0010000) != 0 ){	//0ｷｰ実行
				mode = 1;
			}
			if(url.indexOf("http://") == 0){
				url = url.substring(7,url.length());
			}
			if(mode == 0){
				BrowserConnection conn = (BrowserConnection)Connector.open( "url://" + cushionlink + url);
				conn.connect();
				conn.close();
			}else if(mode == 1){
				//System.out.println(url);
				BrowserConnection conn = (BrowserConnection)Connector.open( "url://" + url );
				conn.connect();
				conn.close();

			}
			stat |= 0x1000000;	//画面更新

		} catch ( Exception e ) {// エラー処理。
		}
	}
	/**
	 * 数値指定用ボックスの表示用フラグ初期化処理
	 */
	public final void NumSet(){
		stat2 &= ~0x0000F00;

		stat |= 0x0000400;
		stat2 |= 0x0002100;	//下二つを結合
		//stat2 |= 0x0000100;	//数字設定
		//stat2 |= 0x0002000;	//数字設定での最も低い数字を1にするフラグ
		data[26] = 1000;	//上限
		data[27] = 0;	//選択している桁
		data[29] = 3;	//選択出来る数字の上限の桁数-1
	}
//	public final void backfromthreadlist(){	//スレッド一覧から(板一覧、ブックマーク)に戻る
	/**
	 * スレッド一覧のはじめまたはスレッドの>>1から(板一覧、スレッド一覧、ブックマーク)に戻る処理。
	 * まず間違いなくバグあり。
	 */
	public final void backfromfirst(){	//スレッド一覧のはじめまたはスレッドの>>1から(板一覧、スレッド一覧、ブックマーク)に戻る
		if( (stat & 0x10000) != 0 ){	//ｽﾚ選択中の時
			if((stat3 & 0x0000020) != 0/*(stat2 & 0x0020000) != 0*/){	//ブックマークからアクセスしてた場合
				stat3 ^= 0x0000020;
				showBookMark(0);
			} else {	//板選択画面に戻る
				ListBox = split( StrList[13][nCacheBrd[nCacheIndex]/*data[3]*/ / 100/*data[22] + data[23]*/], '\t');
				stat |= 0x4000;	//板選択中
				removeCommand(command[6]);
				removeCommand(command[0]);
				addCommand(command[9]);
				addCommand(command[7]);
				//data[19] = ListBox.length;
				if(data[24] + data[25] != nCacheBrd[nCacheIndex]/*data[3]*/ % 100){
					data[10] = nCacheBrd[nCacheIndex]/*data[3]*/ % 100;data[11] = 0;	//場所の読み出し
				} else {
					data[10] = data[24];data[11] = data[25];	//場所の読み出し
				}
				if(data[22] + data[23] != nCacheBrd[nCacheIndex]/*data[3]*/ / 100){
					data[22] = nCacheBrd[nCacheIndex]/*data[3]*/ / 100;data[23] = 0;	//場所の読み出し
				}
				stat |= 0x0000100;
				stat |= 0x1000000;	//画面更新
			}
			stat &= ~0x10000;	//ｽﾚ選択中解除
			stat2 &= ~0x8000000;	//検索結果表示中フラグ
			//data[12] = data[30] + 3;			//LIST Y座標
			//data[13] = height - (data[30] + 3);	//LIST 縦幅
		} else if((stat & 0x0040000) != 0){	//レスを見てたとき
			
			if((stat2 & 0x0020000) != 0){	//ブックマークからアクセスしてた場合(直接スレッドへのブックマーク)
				stat2 &= ~0x0020000;
				//data[77] = 0;
				stat &= ~0x00040000;	//スレを見てるフラグを消去
				showBookMark(0);
			} else {	//スレ選択画面に戻る
				zuzu[0] = 0; //レス番号指定を0にする。
				int i = 1;
				if(data[64] > 0){	//参照元がある場合
					for(i = data[64];i > 0;i--){
						if(Linkref[i*3+1] == 0 && Linkref[i*3] == nCacheBrd[nCacheIndex]){
							stat3 |= 0x0000040;	//戻るでアクセスしている
							data[64] = i;
							i = Linkref[i*3+2];
							break;
						}
						if(i == 1){
							data[64] = 0;
							//i = 1;
							break;
						}
					}
				}
				//System.out.println("i:" + i);
				stat3 |= 0x0000100;	//参照元を保存しない
				httpinit( 1, nCacheBrd[nCacheIndex]/*data[3]*/, i, 0, 0);
				//if((stat3 & 0x0000200) != 0){//参照元を保存していた場合
				//	data[64]--;	//直前に保存した参照元を破棄する
				//}
			/*
				ListBox = StrList[2];	data[12] = height - 13 - (data[30] + 3) * data[56];	data[13] = 13 + (data[30] + 3) * data[56];
				//DivStr = SeparateString(ThreadName[data[5] % data[0]], -1);//sepalateString(ThreadName[data[5] % data[0]]);
				DivStr = FastSeparateByte(ThreadName[data[4]].getBytes());
				data[85] = DivStr.length;
				stat |= 0x10000;
				*/
			}
		}
	}
	/**
	 * ブックマークとブックマークメニュー表示処理
	 * @param mode 0の場合は表示、1場合はメニューを表示
	 */
	public final void showBookMark(int mode){
		if(mode == 0){	//ブックマークの表示
			//if( (stat & 0x40000) != 0 ){
			//removeCommand(command[0]);
			removeCommand(command[6]);
			removeCommand(command[9]);
			//}
			removeCommand(command[0]);
			//data[19] = ListBox.length;
			if((stat2 & 0x0010000) == 0){	//ブックマークが表示されていない
				stat2 |= 0x0010000;	//ブックマークの表示
	//			data[10] = 0;	data[11] = 0;
				data[10] = data[67]; data[11] = data[68];
				addCommand(command[0]);
				addCommand(command[3]);
				//addCommand(command[1]);
				//ListBox = new String[BookMark.length];//BookMark;
			}
			ListBox = tBookMark;
			stat |= 0x0000100;	//リストボックスの表示
			//stat |= 0x1000000;	//画面更新
		} else {	//ブックマークのメニューの表示
			int i = data[10]+data[11];
			//removeCommand(command[0]);
			removeCommand(command[3]);
			removeCommand(command[0]);
			if(/*BookMarkData[i * 3 + 1] == 0 && */BookMark[i].length() == 0/*BookMark[i].getBytes().length == 0*/ || (stat2 & 0x0004000) != 0) {	//スレ番号＝0　板が登録されているor空
//#ifdef 	//
/*				ListBox = new String[5];
				ListBox[0] = "編集";
				ListBox[1] = "隙間を詰める";
				ListBox[2] = "隙間を作る";
				ListBox[3] = "ｴｸｽﾎﾟｰﾄ";
				ListBox[4] = "ｲﾝﾎﾟｰﾄ";
				*/
				ListBox = StrList[16];
//#else
//				ListBox = new String[2];
//				ListBox[0] = "編集";
//				ListBox[1] = "隙間を詰める";
//#endif
				//i = 2;
				stat3 |= 0x8000000;
			} else {	//スレッド
				if(BookMarkData[i * 3 + 1] == 0){	//板
//#ifdef 	//
//					if((stat2 & 0x0004000) != 0){	//function
//						ListBox = new String[7];
//					} else {
//						ListBox = new String[3];
//					}
//#else
//					ListBox = new String[3];
//#endif
//					i = 0;
					ListBox = StrList[18];
				} else {
//#ifdef 	//
//					if((stat2 & 0x0004000) != 0){	//function
//						ListBox = new String[11];
//					} else {
//						ListBox = new String[7];
//					}
//#else
//					ListBox = new String[7];
//#endif
					//ListBox[0] = "編集";
					//ListBox[1] = "ｽﾚｯﾄﾞの検索";
					//ListBox[2] = "書込&終了";
					//ListBox[3] = "最新ﾚｽを読む";
					//ListBox[4] = "消去";
//					ListBox[0] = "最新ﾚｽを読む";
//					ListBox[1] = "ﾚｽ番指定";
//#ifdef 	//java appli
//					ListBox[2] = "書込";
//#else
//					ListBox[2] = "書込&終了";
//#endif
//					ListBox[3] = "書込画面のURL";
//					i = 4;
					ListBox = StrList[17];
				}
//				ListBox[i] = "ｽﾚｯﾄﾞ検索";	i++;
//				ListBox[i] = "編集";	i++;
//				ListBox[i] = "消去";	i++;

//#ifdef 	//
//				if((stat2 & 0x0004000) != 0){	//function
//					ListBox[i] = "隙間を詰める";	i++;
//					ListBox[i] = "隙間を作る";	i++;
//					ListBox[i] = "ｴｸｽﾎﾟｰﾄ";	i++;
//					ListBox[i] = "ｲﾝﾎﾟｰﾄ";	i++;
//				}
//#endif
			}
			data[67] = data[10];	data[68] = data[11];
			data[10] = 0;	data[11] = 0;
			stat3 |= 0x0000400;
			//stat |= 0x1000000;	//再描画
		}
		stat |= 0x1000000;	//画面更新
	}
/*
	public final void ShowBookMarkMenu(){
	}
*/
	/**
	 * ブックマークの位置交換
	 * @param n1 交換対象位置
	 * @param n2 交換先位置
	 */
	public final void ChangeBookMark(int n1,int n2){//ブックマークの位置を交換
		int i;
		String str;

		str = BookMark[n1];
		BookMark[n1] = BookMark[n2];
		BookMark[n2] = str;
		str = tBookMark[n1];
		tBookMark[n1] = tBookMark[n2];
		tBookMark[n2] = str;

		i = BookMarkData[n1 * 3];
		BookMarkData[n1 * 3] = BookMarkData[n2 * 3];
		BookMarkData[n2 * 3] = i;
		i = BookMarkData[n1 * 3+1];
		BookMarkData[n1 * 3+1] = BookMarkData[n2 * 3+1];
		BookMarkData[n2 * 3+1] = i;
		i = BookMarkData[n1 * 3+2];
		BookMarkData[n1 * 3+2] = BookMarkData[n2 * 3+2];
		BookMarkData[n2 * 3+2] = i;
		if(n1 >= data[66] || n2 >= data[66] && data[66] != data[40]){
			data[66]++;
		}
		SaveBookMark(n1);
		SaveBookMark(n2);
		//showBookMark();
	}
	/**
	 * ブックマークの編集
	 * @param mode 0:add 0>:del 0<:上書き
	 * @param str 登録する文字列
	 * @param b 板番号
	 * @param t スレッド番号
	 * @param r レス番号
	 * @return 戻り値 -1:失敗 0<=:成功(登録された位置)
	 */
	//ﾌﾞｯｸﾏｰｸの編集		戻り値 -1:失敗 0<=:成功(登録された位置)
	public final int EditBookMark(int mode/*0:add 0>:del 0<:上書き*/,String str/*登録する文字列*/, int b, int t, int r){
		int i;
		if(mode == 0){
			for(i = 0;i < BookMark.length;i++){
				//if(BookMarkData[i * 3 + 1] == 0 && BookMarkData[i * 3] == 0){//thread & board are 0
				if(BookMarkData[i * 3 + 1] == 0 && BookMark[i].equals("")/*BookMark[i].getBytes().length == 0*/){	//thread = 0 && title = ""
					mode = i + 1;
					strdata[7] = "登録完了";//StrList[10][6] + StrList[10][19];
					stat2 |= 0x0001000;
					break;
				}
			}
		}
		if(mode < 0){	//del
			mode = - 1 - mode;
			strdata[7] = "削除完了";//StrList[10][7] + StrList[10][19];
			stat2 |= 0x0001000;
			BookMark[mode] = "";//"<未使用>";
			tBookMark[mode] = "";//"<未使用>";
			//if((stat2 & 0x0010000) != 0){	//ブックマークが表示されている
			//	showBookMark();
			//}
			BookMarkData[mode * 3] = 0;
			BookMarkData[mode * 3 + 1] = 0;
			BookMarkData[mode * 3 + 2] = 0;
			SaveBookMark(mode);
			if(mode == data[66] - 1){
				data[66]--;
			}
			stat |= 0x1000000;	//画面更新
			return mode;
		} else if(mode > 0){	//add or replace
			mode--;

			if(str.getBytes().length > 140){str = str.substring( 0, 70);}
			BookMark[mode] = str;
			tBookMark[mode] = trimstr(BookMark[mode], width - 29);
			//if((stat2 & 0x0010000) != 0){	//ブックマークが表示されている
			//	showBookMark();
			//}
			BookMarkData[mode * 3] = b;	//board
			BookMarkData[mode * 3 + 1] = t;	//thread
			BookMarkData[mode * 3 + 2] = r;	//res
			///System.out.println(BookMark[mode] + " " + b + " " + t + " " + r);
			if(mode >= data[66]){
				data[66] = mode + 1;
			}
			SaveBookMark(mode);
			stat |= 0x1000000;	//画面更新
			return mode;
		}
		return -1;
	}
	/**
	 * 桁数を計る処理
	 * @param val 対象
	 * @return int値での桁数
	 */
	public final int GetDigits(int val){	//桁数を返す
		//容量重視
		return Integer.toString(val).length();
	}
	/**
	 * キャッシュチェック処理
	 * @see MainCanvas#makeTL
	 * @see MainCanvas#makeRes
	 */
	public final synchronized void chkcache(){
		int i;

		if((stat & 0x40000) != 0){//res
			if(data[6] == nCacheTo[nCacheIndex] - nCacheSt[nCacheIndex]){	//今読んでいるキャッシュの中で最後
				stat3 &= ~0x0808000;
				httpinit(12,nCacheBrd[nCacheIndex],nCacheTo[nCacheIndex] + 1,nCacheTo[nCacheIndex] + 1,nCacheTh[nCacheIndex][0]);
				if((stat3 & 0x0008000) != 0){	//キャッシュが存在する
					stat3 |= 0x0800000;	//接>>を蓄>>にする
				}
			}
			if(data[6] == 0 && nCacheSt[nCacheIndex] > 1){	//今読んでいるキャッシュの中で最初
				//stat3 &= ~0x0808000;
				stat3 &= ~0x0408000;
				httpinit(12,nCacheBrd[nCacheIndex],nCacheSt[nCacheIndex] - 1,nCacheSt[nCacheIndex] - 1,nCacheTh[nCacheIndex][0]);
				if((stat3 & 0x0008000) != 0){	//キャッシュが存在する
					//stat3 |= 0x0800000;	//接>>を蓄>>にする
					stat3 |= 0x0400000;	//<<接を<<蓄にする
				}
			}
			//すでにリンク先がキャッシュされているかチェックする
			if((data[57] & 0x00004000) != 0){
				if(nCacheInfo == null || nCacheInfo.length < data[59]){
					nCacheInfo = new int[data[59]];
				}
				for(i = 0; i < data[59]; i++){
					nCacheInfo[i] = 0;
					stat3 &= ~0x0008000;
					Link(null, i + 1);
					if((stat3 & 0x0008000) != 0){	//キャッシュが存在する
						nCacheInfo[i] = 1;
					}
				}
			}
		} else if( (stat & 0x10000) != 0 ){	//threadlist
//			if(data[4] == nCacheTo[nCacheIndex] - nCacheSt[nCacheIndex]){	//今読んでいるキャッシュの中で最後
				stat3 &= ~0x0808000;
				httpinit(11,nCacheBrd[nCacheIndex],nCacheTo[nCacheIndex] + 1,nCacheTo[nCacheIndex] + 1,0);
				if((stat3 & 0x0008000) != 0){	//キャッシュが存在する
					stat3 |= 0x0800000;	//接>>を蓄>>にする
				}
//			}
//			if(data[4] == 0 && nCacheSt[nCacheIndex] > 1){	//今読んでいるキャッシュの中で最初
				stat3 &= ~0x0408000;
				httpinit(11,nCacheBrd[nCacheIndex],nCacheSt[nCacheIndex] - 1,nCacheSt[nCacheIndex] - 1,0);
				if((stat3 & 0x0008000) != 0){	//キャッシュが存在する
					stat3 |= 0x0400000;	//<<接を<<蓄にする
				}
//			}
			//すでにリンク先がキャッシュされているかチェックする
			if((data[57] & 0x00004000) != 0){
				if(nCacheInfo == null || nCacheInfo.length < data[59]){
					nCacheInfo = new int[data[59]];
				}
				for(i = 0; i < data[59]; i++){
					nCacheInfo[i] = 0;
					stat3 &= ~0x0008000;
					httpinit(12,nCacheBrd[nCacheIndex],1,1,nCacheTh[nCacheIndex][i]);
					if((stat3 & 0x0008000) != 0){	//キャッシュが存在する
						nCacheInfo[i] = 1;
					}
				}
			}
		}
	}
	/**
	 * スレッドリストの作成。受け取ったデータを変換する処理。描画自体はpaint()で行う。
	 * @see MainCanvas#paint
	 */
	public final void makeTL(){ //make thread list
		int i, j, k;
		byte[] b;
		if((stat2 & 0x40000000) != 0){	//makeTLで初期化を行う
			strdata[9] = CacheTitle[nCacheIndex];


			//容量重視
			//i_length = CacheTitle.length;
			for(j = 0;j < /*i_length*/CacheTitle.length;j++){
				if(nCacheTTL[j] >= 0){
					nCacheTTL[j]++;
					//同じ範囲のキャッシュは無効にする
					if((stat & 0x0008000) != 0 && j != nCacheIndex && CacheResData[j] == null && nCacheBrd[j] == nCacheBrd[nCacheIndex] && nCacheSt[j] >= nCacheSt[nCacheIndex] && nCacheTo[j] <= nCacheTo[nCacheIndex]){//キャッシュヒット
						nCacheTTL[j] = -1;
					}
				}
			}
			nCacheTTL[nCacheIndex] = 0;	//キャッシュを有効にする
			data[77] = ((Linklist[data[60]] / 1000) % 10000) * (data[30] + data[34]);	//縦スクロールオフセット
			if((stat & 0x0004000) != 0){	//板選択中の場合
				data[24] = data[10];data[25] = data[11];	//場所の保存
				stat &= ~0x0004000;	//板選択中解除
				removeCommand(command[7]);//検索
				removeCommand(command[9]);//登録
			} else if((stat3 & 0x0000040) != 0){	//戻るときの場合
				data[77] = Linkrefsc[data[64]];	//スクロール分
				data[64]--;
				stat3 ^= 0x0000040;
			} else if((stat2 & 0x0010000) != 0){//ブックマーク
				stat3 |= 0x0000020;	//ブックマークから板にジャンプしたことを示すフラグ
				stat2 ^= 0x0010000;	//ブックマーク解除
				removeCommand(command[3]);
				removeCommand(command[0]);
				//removeCommand(command[1]);
			}
			stat &= ~0x0040000;	//レス表示中のときは解除
			stat |= 0x0010000;	//スレ選択中
			ListBox = StrList[2];//	data[12] = height - 13 - (data[30] + 3) * data[56];	data[13] = 13 + (data[30] + 3) * data[56];
		}
		//初期化
		outarray = new ByteArrayOutputStream(128);
		if(DivStr == null || DivStr.length < 100){
			DivStr = new String[100];
		}
		data[85] = 0;	//DivStrの使用分(DivStrで処理している行)
		data[59] = 0;	//Linklistの使用分
		//i_length = CacheBrdData[nCacheIndex].length;
		int b_length;
		//スレッドリストの描画（実際は描画用変数outarrayへの代入。後にoutarrayの中身をprintにて描画）
		for(j = 0; j < /*i_length*/CacheBrdData[nCacheIndex].length; j++){
			b = Integer.toString(nCacheSt[nCacheIndex] + j).getBytes();
			//スレの並び番号
			outarray.write(b, 0, b.length);
			outarray.write(0x3A);	//:
			//タイトル(レス数)
			b = (CacheBrdData[nCacheIndex][j] + "(" + Integer.toString(nCacheBrdData[nCacheIndex][j]) + ")").getBytes();
			int w = GetDigits(nCacheSt[nCacheIndex] + j) + 1;
			data[61] = w;	//Linkバイト数
			data[62] = 0;			//Linkのスタート桁
			data[63] = data[85];	//Linkのスタート行
			addLinklist();
			b_length = b.length;
			for(i = 0;i < b_length;i++){
				//	0x81					0x9F	0xE0					0xFE
				//if((-127 <= b[i] && b[i] <= -97) || (-32 <= b[i] && b[i] <= -2)){//SJISの１バイト目
				if(0 > b[i] && (b[i] <= -97 || -32 <= b[i])){//SJISの１バイト目(高速モード(判定ミスが起こるかも))
					w += 2;
					if(data[92] != 1 && w > data[42]){//決められた字数を越えてしまったら
						addDivStr2();
						w = 2;
						if(data[92] == 2){
							for(k = 0; k < data[61]; k++){
								outarray.write(' ');
							}
							w += data[61];
						}
					}

					outarray.write(b[i]);	i++;
					outarray.write(b[i]);
				} else {//1byte
					w++;
					if(data[92] != 1 && w > data[42]){//決められた字数を越えてしまったら
						addDivStr2();
						w = 1;
						if(data[92] == 2){
							for(k = 0; k < data[61]; k++){
								outarray.write(' ');
							}
							w += data[61];
						}
					}
					outarray.write(b[i]);
				}
			}
			if(w != 0){
				addDivStr2();
			}
		}
//		data[60] = data[4];
//		data[77] = ((Linklist[data[60]] / 1000) % 10000) * (data[30] + data[34]);	//縦スクロールオフセット
		if((data[57] & 0x00008000) == 0){	//スレごとのスクロールをしていない場合
			i = (data[85] + 5/*2*/) * (data[30] + data[34]) - height;
			if(data[77] > i){data[77] = i;}
		}
		if(data[77] < 0){data[77] = 0;}
		data[48] = 0;	//横スクロールオフセット
//		if(data[59] != 0){		//Linklistの使用分
//			data[60] = 0;		//Linkfocus
//		} else {
//			data[60] = -1;		//Linkfocus
//		}
		stat &= ~0x0000100;	//リストボックス解除
		//stat |= 0x1000000;	//画面更新
		//addCommand(command[9]);//登録
		addCommand(command[0]);//ﾒﾆｭｰ
		addCommand(command[6]);//戻る
		data[10] = 0;data[11] = 0;
		chkcache();	//キャッシュチェック
	}
	//public final void FastMaRE(/*int option*/){	//FastMakeResElements
	/**
	 * レスの作成。受け取ったデータを変換する処理。描画自体はprint()で行う。
	 */
	public final void makeRes(){
		int i, j, k, n;
		data[77] = -data[30];	//縦スクロールオフセット
		data[48] = 0;			//横スクロールオフセット
		if((stat2 & 0x80000000) != 0){	//makeResで初期化を行う
			strdata[9] = CacheTitle[nCacheIndex];
			i_length = CacheTitle.length;
			for(j = 0;j < i_length;j++){
				if(nCacheTTL[j] >= 0){
					nCacheTTL[j]++;
					if(CacheBrdData[j] == null && nCacheTh[j] != null && nCacheTh[j][0] == nCacheTh[nCacheIndex][0] && nCacheBrd[j] == nCacheBrd[nCacheIndex]){
						nCacheAll[j] = nCacheAll[nCacheIndex];	//All数を最新のものに更新
					}
				}
			}
			nCacheTTL[nCacheIndex] = 0;	//キャッシュを有効にする
			//自動しおり機能
			j = data[67] + data[68];
			
			if((data[57] & 0x00000200) != 0 && nCacheBrd[nCacheIndex]/*data[3]*/ == BookMarkData[j * 3] && nCacheTh[nCacheIndex][0]/*data[2]*/ == BookMarkData[j * 3 + 1]){
				if(BookMarkData[j * 3 + 2] >= 0 && BookMarkData[j * 3 + 2] <= nCacheTo[nCacheIndex]/*data[8]*/){
					BookMarkData[j * 3 + 2] = nCacheTo[nCacheIndex]/*data[8]*/ + 1;
					SaveBookMark(j);
				}
			}
			if((stat3 & 0x0000040) != 0){	//戻るときの場合
				data[77] = Linkrefsc[data[64]];	//スクロール分
				data[64]--;
				stat3 ^= 0x0000040;
			} else if((stat2 & 0x0010000) != 0){//ブックマーク
				stat2 |= 0x0020000;	//ブックマークからスレッドにジャンプしたことを示すフラグ
				stat2 ^= 0x0010000;	//ブックマーク解除
				removeCommand(command[3]);
				removeCommand(command[0]);
			}
			stat &= ~0x0010000;	//ｽﾚ選択中のときは解除
			stat |= 0x0040000;	//レス表示中
			ListBox = StrList[3];
		}
		stat2 &= ~0x80000000;
		//alist = new ArrayList();
		outarray = new ByteArrayOutputStream(128);
		//String str;
		//ResElements = new String[1];
		while(CacheResData[nCacheIndex][data[6]] == null/*data[45] < data[6] + 1*/){
//			try{Thread.sleep(100);}catch (Exception e){}
			wait(100);
		}
		byte b[] = CacheResData[nCacheIndex][data[6]]/*Res[data[6]]*//*.getBytes()*/, b2[], b3[];
//#ifdef DEBUG
//		System.out.println(new String(b));
//#endif
		//ﾃｷｽﾄﾎﾞｯｸｽ用
		//resdata = "";
		//resdata = resdata + new String(b);
		resdata = b;

		//if(resdata.indexOf(ngword) != -1){

		/*if(ngword.indexOf('|') != -1){
			int ngi = 0;
			int abon = 0;
			while(ngword.indexOf('|',ngi) != - 1 || abon != 1) {
				ngi = ngword.indexOf('|',ngi);
				String ngstr = ngword.substring(ngword.indexOf('|',ngi + 1));
				if(resdata.indexOf(ngstr) != -1){
					abon = 1;
				}
			}
			if(abon == 1){
				String newstr = resdata.substring(resdata.lastIndexOf('\r'));
				newstr = newstr.substring(newstr.indexOf('\t'));
				newstr = "あぼーん\n" + newstr;
			//System.out.println(newstr);
				b =  newstr.getBytes();
			}
		}*/
		//	b = "あぼーん\nあぼーん\tあぼーん\tあぼーん".getBytes();
		//}
		//初期化
		//if(DivStr == null){
		//	DivStr = new String[100];
		//}
		if(iDivStr == null || iDivStr.length < 100){
			iDivStr = new int[100];
		}
		data[86] = 0;	//現在の行の文字数(iDivStrで使用する変数)
		data[85] = 0;	//DivStrの使用分(DivStrで処理している行)
		data[59] = 0;	//Linklistの使用分
		//if(Linklist == null){
		//	Linklist = new int[20];
		//}
		//本文
		i = SubMaRE(b, 0, 0);
		//区切り線
		n = GetDigits(nCacheAll[nCacheIndex]/*data[9]*/);
		k = (width - (n + 10/*4 + 2*/) * data[33]) / data[30];
		//str = "－";
		b2 = /*str*/"―".getBytes();


		for(j = 0;j < k;j++){
			outarray.write(b2, 0, b2.length);
		}
		//str = "All:" + data[9];
		b3 = ("AllRes:" + nCacheAll[nCacheIndex]/*data[9]*/).getBytes();
		outarray.write(b3, 0, b3.length);
		j += b3.length;
		outarray.write(b2, 0, b2.length);
		j++;
		if(k * data[30] + (n + 7/*4 + 2 + 1*/) * data[33] < width){
			outarray.write(45);
			j++;
		}
		data[86] += j;
		addDivStr();
		//名前　メール　時間　ＩＤ
		//EZPLUS
		//resstr = resstr + "\r名前:"/*StrList[10][15]*/ + ResElements[0] + "\r\uE521 " + ResElements[1] + "\r\uE56A "/*StrList[10][16]*/ + ResElements[2];
		//
		//resstr = resstr + "\r\uE301 "/*StrList[10][15]*/ + ResElements[0] + "\r\uE101 " + ResElements[1] + "\r\uE024 "/*StrList[10][16]*/ + ResElements[2];
		//DOJA
		//resstr = resstr + "\r\ue6b1 "/*StrList[10][15]*/ + ResElements[0] + "\r\ue6d3 " + ResElements[1] + "\r\ue6ba "/*StrList[10][16]*/ + ResElements[2];
		j = i;

		//str = "\ue6b1 ";
		b2 = Emoji[0].getBytes();//str.getBytes();
		outarray.write(b2, 0, b2.length);
		data[86] += 2;
		i = SubMaRE(b, i, 3);

		//outarray.write(b, j, i - j - 1);
		//ResElements[0] = outarray.toString();	//名前
		strdata[3] = new String(b, j, i - j - 1);	//名前
		//outarray.reset();
		//str = "\ue6d3 ";
		/*for(int i_a=0;i_a >= b_length;i_a++){
			name_a[i_a] = strdata[3].substring(i_a, data[42]);
			System.out.print(name_a[i_a]);
			i_a = data[42];
		}*/


		b2 = Emoji[1].getBytes();//str.getBytes();
		outarray.write(b2, 0, b2.length);
		data[86] += 2;
		i = SubMaRE(b, i, 3);
		//str = "\ue6ba ";
		b2 = Emoji[2].getBytes();//str.getBytes();
		outarray.write(b2, 0, b2.length);
		data[86] += 2;
		i = SubMaRE(b, i, 3);




		if(i < b.length){	//IDあり
			b2 = Emoji[3].getBytes();
			outarray.write(b2, 0, b2.length);
			data[86] += 3;
			i = SubMaRE(b, i, 3);


/*
*/
//		} else if(ResElements.length >= 4){
//			//resstr = ResElements[3] + resstr;
		}
		//DivStr = SeparateString(resstr, -1);
//		if( (stat2 & 0x0000080) != 0 ){	//AA MODE
//			DivStr = FastAASeparateByte(resstr.getBytes(), '\r');
//		} else {
//			DivStr = FastSeparateByte(resstr.getBytes());
//		}
		//DivStr = new String[vec.size()];
		//vec.copyInto(DivStr);
		//DivStr = alist.toArray();
		//data[85]--;
		resstr = outarray.toString();



		if((stat2 & 0x0000002) == 0){	//表示設定ではないとき
			stat &= ~0x0000100;	//リストボックス解除
		}
		addCommand(command[0]);//メニュー
		addCommand(command[6]);//戻る
		if(data[59] != 0){		//Linklistの使用分
			data[60] = 0;		//Linkfocus
			if(data[77] != -data[30]){
				setLink();
			}
		} else {
			data[60] = -1;		//Linkfocus
		}
		//data[65] = 0;	//Linkの選択スキップ分
		chkcache();	//キャッシュチェック

		stat |= 0x1000000;	//画面更新

	}
	/**
	 * makeResで行うLink判別、改行判別等々の何度も行う処理をメソッドとして分割してあるもの
	 * @param b data
	 * @param i offset
	 * @param w 不明
	 * @return i - offset(詳細不明)
	 */
	public final int SubMaRE(byte b[], int i, int w){	//b:data i:offset
//		if((stat2 & 0x0000080) == 0){	//!AA MODE
		i_length = b.length;
		for(;i < i_length;i++){
			if((stat3 & 0x0000003) != 0){	//LinkON
				if((stat3 & 0x0000001) != 0){	//LinkON(同じスレッド内)
					if((b[i] < 48 || b[i] > 57) && b[i] != 45){	//リンクに許されているものではない場合
						if(/*data[61] > 2 && */b[i-1] != 62 && b[i-1] != 132){
							Linkurllist[data[59]] = new String(b, i-data[61], data[61]);
							addLinklist();
						}
						stat3 ^= 0x0000001;
					} else {
						data[61]++;
					}
//				} else if((stat3 & 0x0000002) != 0){	//LinkON(URL)
				} else {	//LinkON(URL)
					if((b[i] > 42 && b[i] < 127) || (b[i] > 34 && b[i] < 39)){	//リンクに許されているものの場合
						data[61]++;
					} else {
						if(data[61] > 10){
							//ttpでのリンクの際に起こる不具合を防止
							byte b2 = b[i-data[61]];
							b[i-data[61]] = 104;	//h
							Linkurllist[data[59]] = new String(b, i-data[61], data[61]);
							b[i-data[61]] = b2;
							addLinklist();
						}
						stat3 ^= 0x0000002;
					}
				}
			}
			//	0x81					0x9F	0xE0					0xFE
			//if((-127 <= b[i] && b[i] <= -97) || (-32 <= b[i] && b[i] <= -2)){//SJISの１バイト目
			if(0 > b[i] && (b[i] <= -97 || -32 <= b[i])){//SJISの１バイト目(高速モード(判定ミスが起こるかも))
				w += 2;
				if(w > data[42] && (stat2 & 0x0000080) == 0){//決められた字数を越えてしまったら
					addDivStr();
					w = 2;
				}

				outarray.write(b[i]);	i++;
				outarray.write(b[i]);
			} else if(b[i] == '\r'/* || b[i] == '\n'*/){
				addDivStr();	w = 0;
				continue;
			} else if(b[i] == '\t'){
				i++;
				break;
			} else {//1byte
				w++;
				//if(i+1 < b.length && (b[i] == 62 || (b[i] == 129 && b[i+1] == 132))){// >
				if(b[i] == 62 && i+1 < b.length){// >xxx or >>xxx
					if(w+1 > data[42] && (stat2 & 0x0000080) == 0){//決められた字数を越えてしまったら
						addDivStr();
						w = 1;
					}

					//if(w != 0){addDivStr();	w = 0;}
					stat3 |= 0x0000001;	data[63] = data[85];
					Linklist2[data[59]] = data[86];
					data[62] = w-1;
					//if(b[i+1] == 62 || b[i+1] == 132){
					if(b[i+1] == 62){	// >>xxx
						data[61] = 2;
						outarray.write(b[i]);	i++;	w++;
						data[86]++;
					} else {
						data[61] = 1;
					}
//以下の内、容量の小さい方を利用
				} else if(i+10 < b.length && b[i+1] == 116 && ((b[i] == 104 && b[i+2] == 116 && b[i+3] == 112) || (b[i] == 116 && b[i+2] == 112))){	//http or ttp
					if(w+3 > data[42] && (stat2 & 0x0000080) == 0){//決められた字数を越えてしまったら
						addDivStr();
						w = 1;
					}
					//if(w != 0){addDivStr();	w = 0;}
					stat3 |= 0x0000002;	data[61] = 4;	data[63] = data[85];
					Linklist2[data[59]] = data[86];
					data[62] = w-1;	//Linkのスタート桁
					//if(b[i] == 116){	//ttp
					//	outarray.write(104);	w++;	//h
					//} else {
					//	outarray.write(b[i]);	i++;	w++;
					//}
					//w += 2;
					if(b[i] == 104){	//http
						i++;
					}
					outarray.write(104);	//h

					w += 3;
					outarray.write(b[i]);	i++;//	w++;	//t
					outarray.write(b[i]);	i++;//	w++;	//t
					data[86] += 3;
/*
				} else if(b[i] == 104 && i+10 < b.length && b[i+1] == 116 && b[i+2] == 116 && b[i+3] == 112){	//http
					//if(w != 0){addDivStr();	w = 0;}
					stat3 |= 0x0000002;	data[62] = w;	data[61] = 4;	data[63] = data[85];
					outarray.write(b[i]);	i++;	w++;
					outarray.write(b[i]);	i++;	w++;
					outarray.write(b[i]);	i++;	w++;
				} else if(b[i] == 116 && i+10 < b.length && b[i+1] == 116 && b[i+2] == 112){	//ttp
					//if(w != 0){addDivStr();	w = 0;}
					stat3 |= 0x0000002;	data[62] = w;	data[61] = 4;	data[63] = data[85];
					outarray.write(104);
					outarray.write(b[i]);	i++;	w++;
					outarray.write(b[i]);	i++;	w++;
*/				} else {
					if(w > data[42] && (stat2 & 0x0000080) == 0){//決められた字数を越えてしまったら
						addDivStr();
						w = 1;
					}
				}
				outarray.write(b[i]);
			}
			data[86]++;
		}
//		} else {
//			for(;i < b.length;i++){
//				if(b[i] == '\r'/* || b[i] == '\n'*/){
//					//alist.addElement(outarray.toString());
//					addDivStr();
//					w = 0;
//				} else if(b[i] == '\t'){
//					i++;
//					break;
//				} else {//1byte
//					outarray.write(b[i]);
//					w++;
//				}
//			}
//		}

		if(w != 0){
			//alist.addElement(outarray.toString());
			addDivStr();
			//w = 0;
		}
		//outarray.reset();
//		System.out.println("i:" + i);
		return i;
	}

	public final void addDivStr2(){
		DivStr[data[85]] = outarray.toString();	data[85]++;
		outarray.reset();
		if(data[85] == DivStr.length){
			String[] tmp = new String[DivStr.length*2];
			//int i;
			//for(i = 0;i < DivStr.length;i++)
			//	tmp[i] = DivStr[i];
			System.arraycopy(DivStr, 0, tmp, 0, data[85]);
			DivStr = tmp;
		}
	}

	public final void addDivStr(){
		if(data[85] == 0){
			iDivStr[data[85]] = data[86];/*outarray.size()*/
		} else {
			iDivStr[data[85]] = iDivStr[data[85]-1] + data[86];/*outarray.size()*/
		}
		data[86] = 0;
		data[85]++;
		if(data[85] == iDivStr.length){
			int[] tmp = new int[iDivStr.length*2];
			//int i;
			//for(i = 0;i < DivStr.length;i++)
			//	tmp[i] = DivStr[i];
            System.arraycopy(iDivStr, 0, tmp, 0, data[85]);
			iDivStr = tmp;
		}


	}

	/**
	 * リンク用配列にリンクを追加。リンクの指定は全てグローバル変数を使用。
	 */
	public final void addLinklist(){
		if(data[61] > 213){data[61] = 213;}
		Linklist[data[59]] = (data[61] * 10000 + data[63]) * 1000 + data[62];
		//Linkurllist[data[59]] = null;
		data[59]++;
		if(data[59] == Linklist.length){
			int[] tmp = new int[Linklist.length*2];
			//int i;
			//for(i = 0;i < Linklist.length;i++)
			//	tmp[i] = Linklist[i];
            System.arraycopy(Linklist, 0, tmp, 0, data[59]);
			Linklist = tmp;

			tmp = new int[Linklist.length*2];
            System.arraycopy(Linklist2, 0, tmp, 0, data[59]);
			Linklist2 = tmp;
			String[] tmp2 = new String[Linklist.length*2];
            System.arraycopy(Linkurllist, 0, tmp2, 0, data[59]);
			Linkurllist = tmp2;
		}
	}

	/**
	 * 表示している領域にリンクがあるかどうかを判定？
	 */
	public final void setLink(){
		int i, j, k;
		k = data[77] / (data[30] + data[34]);//	if(0 > i){i = 0;}	//DivStrの書き始め
		for(j = 0;j < data[59];j++){
			i = (Linklist[j] / 1000) % 10000;
			if(i > k/*i >= k*/){	//リンク張るところは表示内にある場合
				data[60] = j;
				break;
			}
		}
/*
		k = data[77] / (data[30] + data[34]);//	if(0 > i){i = 0;}	//DivStrの書き始め
		for(j = 0;j < data[59];j++){
			i = (Linklist[j] / 1000) % 10000;
			if(i > k){	//リンク張るところは表示内にある場合
				data[60] = j;
				break;
			}
		}
*/
	}

	/**
	 * ボックスの形での文字列表示処理
	 * @param g Graphics
	 * @param x x座標
	 * @param y y座標
	 * @param str 複数行に渡る場合。こちらを使用したい場合はstr2=nullの必要がある。
	 * @param str2 1行のみの場合。
	 * @param yspace 上下幅
	 * @return ret
	 */
//str:複数行にわたる場合　str2:１行のみの場合 strを使用したい場合は、str2=nullの必要があります。
	public final int DrawBoxStr(Graphics g , int x , int y , String[] str , String str2 , int yspace/*上下幅*/){
		int i = 16 * data[33], ret = 0;
		if(str2 != null){
			str = new String[1];	str[0] = str2;
			i = str2.getBytes().length * data[33];
			ret = x + i / 2;//- i / 2 + i;
		}
		int len = str.length;

		if(i % 2 == 1){i++;}
		x -= i / 2;
		y -= data[30]*len/2;

		g.setColor(ColScm[6],ColScm[7],ColScm[8]);	//薄い色 192,192,192
		g.fillRect( x + 1, y - 1 - yspace, i + 3, data[30]*len + 4 + yspace * 2);
		g.setColor(ColScm[0],ColScm[1],ColScm[2]);//2chのスレ背景色 239,239,239
		g.fillRect( x - 1, y - 2 - yspace, i + 2, data[30]*len + 2 + yspace * 2);

		g.setColor(ColScm[9],ColScm[10],ColScm[11]);	//中間の濃さ 128,128,128
		g.drawRect( x - 2, y - 3 - yspace, i + 3, data[30]*len + 3 + yspace * 2);
		//角を丸める
		//g.drawLine( x - 1, y - 3 - yspace, x + i, y - 3 - yspace);//上
		//g.drawLine( x - 1, y + data[30]*len + yspace, x + i, y + data[30]*len + yspace);//下
		//g.drawLine( x - 2, y - 2 - yspace, x - 2, y + data[30]*len + yspace - 1);//左
		//g.drawLine( x + i + 1, y - 2 - yspace, x + i + 1, y + data[30]*len + yspace);//右

		g.setColor(ColScm[3],ColScm[4],ColScm[5]);	//文字の色 0,0,0
		for(i = 0;i < len;i++){
			g.drawString(str[i], x, y + data[30] * i - 1, 20/*g.TOP|g.LEFT*/);
		}
		return ret;
	}

	/**
	 * フォントの指定。現在、全ての機種で太文字などは意味をなしていない。やり方が悪いかも。
	 */
	public final void SetFont(){
		int i;
		if(data[36] == 0){	//SMALL
			//font = Font.getFont(Font.FACE_MONOSPACE,Font.STYLE_BOLD,Font.SIZE_SMALL);
			if(data[98] == 0){
				if(data[99] == 0){ //PLAIN
					font = font.getFont(font.FACE_SYSTEM,font.STYLE_PLAIN,font.SIZE_SMALL);
				}else if(data[99] == 1){
					font = font.getFont(font.FACE_MONOSPACE,font.STYLE_PLAIN,font.SIZE_SMALL);
				}else if(data[99] == 2){
					font = font.getFont(font.FACE_PROPORTIONAL,font.STYLE_PLAIN,font.SIZE_SMALL);
				}
			}else if(data[98] == 1){ //BOLD
				if(data[99] == 0){
					font = font.getFont(font.FACE_SYSTEM,font.STYLE_BOLD,font.SIZE_SMALL);
				}else if(data[99] == 1){
					font = font.getFont(font.FACE_MONOSPACE,font.STYLE_BOLD,font.SIZE_SMALL);
				}else if(data[99] == 2){
					font = font.getFont(font.FACE_PROPORTIONAL,font.STYLE_BOLD,font.SIZE_SMALL);
				}
			}else if(data[98] == 2){
				if(data[99] == 0){ //ITALIC
					font = font.getFont(font.FACE_SYSTEM,font.STYLE_ITALIC,font.SIZE_SMALL);
				}else if(data[99] == 1){
					font = font.getFont(font.FACE_MONOSPACE,font.STYLE_ITALIC,font.SIZE_SMALL);
				}else if(data[99] == 2){
					font = font.getFont(font.FACE_PROPORTIONAL,font.STYLE_ITALIC,font.SIZE_SMALL);
				}
			}

		} else if(data[36] == 2) {	//LARGE
			//font = Font.getFont(Font.FACE_MONOSPACE,Font.STYLE_BOLD,Font.SIZE_LARGE);
			if(data[98] == 0){
				if(data[99] == 0){
					font = font.getFont(font.FACE_SYSTEM,font.STYLE_PLAIN,font.SIZE_LARGE);
				}else if(data[99] == 1){
					font = font.getFont(font.FACE_MONOSPACE,font.STYLE_PLAIN,font.SIZE_LARGE);
				}else if(data[99] == 2){
					font = font.getFont(font.FACE_PROPORTIONAL,font.STYLE_PLAIN,font.SIZE_LARGE);
				}
			}else if(data[98] == 1){
				if(data[99] == 0){
					font = font.getFont(font.FACE_SYSTEM,font.STYLE_BOLD,font.SIZE_LARGE);
				}else if(data[99] == 1){
					font = font.getFont(font.FACE_MONOSPACE,font.STYLE_BOLD,font.SIZE_LARGE);
				}else if(data[99] == 2){
					font = font.getFont(font.FACE_PROPORTIONAL,font.STYLE_BOLD,font.SIZE_LARGE);
				}
			}else if(data[98] == 2){
				if(data[99] == 0){
					font = font.getFont(font.FACE_SYSTEM,font.STYLE_ITALIC,font.SIZE_LARGE);
				}else if(data[99] == 1){
					font = font.getFont(font.FACE_MONOSPACE,font.STYLE_ITALIC,font.SIZE_LARGE);
				}else if(data[99] == 2){
					font = font.getFont(font.FACE_PROPORTIONAL,font.STYLE_ITALIC,font.SIZE_LARGE);
				}
			}
		} else {//if(data[36] == 1){	//MIDIUM
			//font = Font.getFont(Font.FACE_MONOSPACE,Font.STYLE_BOLD,Font.SIZE_MEDIUM);
			if(data[98] == 0){
				if(data[99] == 0){
					font = font.getFont(font.FACE_SYSTEM,font.STYLE_PLAIN,font.SIZE_MEDIUM);
				}else if(data[99] == 1){
					font = font.getFont(font.FACE_MONOSPACE,font.STYLE_PLAIN,font.SIZE_MEDIUM);
				}else if(data[99] == 2){
					font = font.getFont(font.FACE_PROPORTIONAL,font.STYLE_PLAIN,font.SIZE_MEDIUM);
				}
			}else if(data[98] == 1){
				if(data[99] == 0){
					font = font.getFont(font.FACE_SYSTEM,font.STYLE_BOLD,font.SIZE_MEDIUM);
				}else if(data[99] == 1){
					font = font.getFont(font.FACE_MONOSPACE,font.STYLE_BOLD,font.SIZE_MEDIUM);
				}else if(data[99] == 2){
					font = font.getFont(font.FACE_PROPORTIONAL,font.STYLE_BOLD,font.SIZE_MEDIUM);
				}
			}else if(data[98] == 2){
				if(data[99] == 0){
					font = font.getFont(font.FACE_SYSTEM,font.STYLE_ITALIC,font.SIZE_MEDIUM);
				}else if(data[99] == 1){
					font = font.getFont(font.FACE_MONOSPACE,font.STYLE_ITALIC,font.SIZE_MEDIUM);
				}else if(data[99] == 2){
					font = font.getFont(font.FACE_PROPORTIONAL,font.STYLE_ITALIC,font.SIZE_MEDIUM);
				}
			}
		}
		//font = Font.getFont(Font.FACE_SYSTEM,Font.STYLE_BOLD,Font.SIZE_SMALL);

		data[30] = font.getHeight();
		if(data[30] % 2 == 1){
			data[30]++;
		}

		data[31] = font.getBaselinePosition();
		data[32] = font.getHeight() - font.getBaselinePosition();
		data[33] = font.charWidth('M');

		data[12] = data[30] + 3;			//LIST Y座標
		data[13] = height - (data[30] + 3);	//LIST 縦幅
		data[42] = width / data[33];	//一行の文字(byte)数
		//高速化のためにトリミング済みのブックマーク名をあらかじめ作成
		for(i = 0;i < data[66];i++){
			tBookMark[i] = trimstr(BookMark[i], width - 29);
		}
	}

	/**
	 * 矢印代わりの三角形を描画
	 * @param g Graphics
	 * @param x x座標
	 * @param y y座標
	 * @param direction 0:下 1:上
	 */
	public final void DrawTriangle(Graphics g, int x, int y, int direction /*0:下 1:上*/){
		int i;
		for (i = 2; i >= 0; i--) {
			if(direction == 0){
				g.drawLine(x - i, y + 2 - i, x + i, y + 2 - i);
			} else {
				g.drawLine(x - i, y + 2 + i, x + i, y + 2 + i);
			}
		}
	}

	/**
	 * 通信処理の入り口。ここからthttpget()に飛んでさらにhttpgetdata()に飛ぶ
	 * @param mode 0:板一覧の読込 1:スレッドリスト 2:レス (3:Menu) 4:スレ検索 5:URL直接指定 6:板のURLを取得 7:書き込み処理 11:スレッドリスト(キャッシュチェック) 12:レス(キャッシュチェック)
	 * @param brd board
	 * @param st start(1～)
	 * @param to end
	 * @param th thread
	 * @see MainCanvas#thttpget()
	 * @see MainCanvas#httpgetdata()
	 */
	public final void httpinit(int mode/*0:板一覧の読込 1:スレッドリスト 2:レス (3:Menu) 4:スレ検索 5:URL直接指定 6:板のURLを取得 7:書き込み処理 11:スレッドリスト(キャッシュチェック) 12:レス(キャッシュチェック)*/,
	 					int brd/*board*/, int st/*start(1～)*/, int to/*end*/, int th/*thread*/){
						//最新レス取得時はst=-1,to=取得するレスの数
		int i, j;
		int cachestat = 0;
		int savelinkref = 0;
		//data[72] = 0;
		if(mode < 10){

			stat3 &= ~0x0000200;	//参照元を保存しているフラグを初期化
			//戻るときでない、参照元を保存しないフラグがたっていない、再読込でないときに参照元を保存
			if(/*(mode == 1 || mode == 2 || mode == 4 || mode == 5)*/mode >= 1 && mode <= 5 && (stat & 0x0008000) == 0 && (stat3 & 0x0000140) == 0/*(stat3 & 0x0000040) == 0 && (stat3 & 0x0000100) == 0*/ ){
				//addLinkref();	//参照元の追加
				//暫定仕様
				//ブックマークまたは板リストからジャンプするときは初期化
				if((stat2 & 0x0010000) != 0 || (stat & 0x0004000) != 0){
					data[64] = 0;
				} else {
					i = data[64]+1;
					//data[64]++;
					if( (i+1)*3 > Linkref.length){
						int[] tmp = new int[Linkref.length*2];
			            System.arraycopy(Linkref, 0, tmp, 0, Linkref.length);
						Linkref = tmp;
						tmp = new int[Linkrefsc.length*2];
			            System.arraycopy(Linkrefsc, 0, tmp, 0, Linkrefsc.length);
						Linkrefsc = tmp;
					}
					Linkref[i*3  ] = nCacheBrd[nCacheIndex];//data[3];	//b
					if((stat & 0x0010000) != 0){	//スレ選択中
						Linkref[i*3+1] = 0;	//thread
						Linkref[i*3+2] = nCacheSt[nCacheIndex] + data[60];	//r
					} else {
						Linkref[i*3+1] = nCacheTh[nCacheIndex][0];//data[2]nThread[data[4]];	//thread
						Linkref[i*3+2] = nCacheSt[nCacheIndex] + data[6];//data[7];	//r
					}
					Linkrefsc[i] = data[77];	//スクロール分
					savelinkref = 1;
				}
			}
			stat3 &= ~0x0000100;	//参照元を保存しないフラグを消す
		}
		//スレ番号が0の時はスレッドリスト読み込みに修正する
		if(mode % 10 == 2 && th == 0){mode -= 1;}
		if((mode == 2 || mode == 12) && st > 0){
			//現在見てるレスの中にジャンプ先があるとき
			//i_length = CacheTitle.length - 1;
			for(i = CacheTitle.length - 1;i >= 0;i--){	//キャッシュ内の検索
//				#ifdef DEBUG
//				System.out.println("CACHE:check" + i + "/" + CacheTitle.length + " /br" + nCacheBrd[i] + "/st" + nCacheSt[i] + "/to" + nCacheTo[i]);
//				#endif
				if(nCacheTTL[i] >= 0 && CacheBrdData[i] == null && nCacheTh[i] != null && nCacheTh[i][0] == th && nCacheBrd[i] == brd){
//					#ifdef DEBUG
//					System.out.println("CACHE:thread cachest:" + nCacheSt[i] + " cacheto:" + nCacheTo[i] + " " + stat3);
//					#endif
					if( ((stat3 & 0x0000010) == 0 && nCacheSt[i] <= st && st <= nCacheTo[i])
							|| ((stat3 & 0x0000010) != 0 && nCacheSt[i] <= to && to <= nCacheTo[i]) ){//キャッシュヒット
						if(mode == 12){
							stat3 |= 0x0008000;	//キャッシュが存在する
							return;
						}
						if((stat3 & 0x0000080) != 0){//先読みをすることを示すフラグ
							stat3 &= ~0x0000080;	//先読みをすることを示すフラグ
							return;
						}
						nCacheIndex = i;
						if((stat3 & 0x0000010) != 0){	//終わりから読む
							data[6] = to - nCacheSt[i];
						} else {
							data[6] = st - nCacheSt[i];
						}
						stat3 &= ~0x0000010;	//レスを最後から読むフラグの削除
						data[45] = nCacheTo[i] - nCacheSt[i] + 1;
						stat2 |= 0x80000000;	//makeResで初期化を行う
						makeRes();
						cachestat = 1;
						break;
					} else if( (stat3 & 0x0000010) == 0 && nCacheSt[i] <= to && to <= nCacheTo[i] ){
						to = nCacheSt[i] - 1;
					} else if( (stat3 & 0x0000010) != 0 && nCacheSt[i] <= st && st <= nCacheTo[i] ){
						st = nCacheTo[i] + 1;
					}
				}
			}
			if(mode == 12){return;}
			//for(i = CacheTitle.length;i > 0;i--){
			//}
		//板を見たい場合のキャッシュ
		} else if((mode == 1 || mode == 11) && (stat & 0x0008000) == 0/*再読込の時はキャッシュを使用しない*/) {
			//i_length = CacheTitle.length - 1;
			for(i = CacheTitle.length - 1;i >= 0;i--){	//キャッシュ内の検索
				if(nCacheTTL[i] >= 0 && CacheResData[i] == null && nCacheBrd[i] == brd
				 && ( ((stat3 & 0x0000010) == 0 && nCacheSt[i] <= st && st <= nCacheTo[i])
				 || ((stat3 & 0x0000010) != 0 && nCacheSt[i] <= to && to <= nCacheTo[i]) )){//キャッシュヒット
					if(mode == 11){
						stat3 |= 0x0008000;	//キャッシュが存在する
						return;
					}
					//data[5] = st - 1;
					//for(j = 0;j < CacheTitle.length;j++){
					//	if(nCacheTTL[j] >= 0){
					//		nCacheTTL[j]++;
					//	}
					//}
					//nCacheTTL[i] = 0;
					nCacheIndex = i;
/*
					ThreadName = CacheBrdData[i];
					data[3] = brd;
					nRes = nCacheBrdData[i];
					nThread = nCacheTh[i];
*/
					//strdata[9] = CacheTitle[i];
					if((stat3 & 0x0000010) != 0){	//戻ってるとき
						//data[5] = to - 1;
						data[60] = to - nCacheSt[i];
						//data[4] = to - nCacheSt[i];
					} else {
						//data[5] = st - 1;
						data[60] = st - nCacheSt[i];
						//data[4] = st - nCacheSt[i];
					}
					stat3 &= ~0x0000010;	//レスを最後から読むフラグの削除
					stat2 &= ~0x0000080;	//AAMODE解除
					stat2 |= 0x40000000;	//makeTLで初期化を行う
					makeTL();
					stat |= 0x1000000;	//画面更新
					//stat |= 0x0000100;
					cachestat = 1;
					break;
					//return;
				}
			}
			if(mode == 11){return;}
		}
		//参照元を保存する場合
		if(savelinkref == 1 && (cachestat == 1 || (cachestat != 1 && (stat & 0x0010) == 0))){
			data[64]++;
			stat3 |= 0x0000200;	//参照元を保存した
		}
		if(cachestat == 1 || (stat & 0x0010) != 0){	//通信中orキャッシュを読む場合
			return;	//通信は行わない
		}
		stat &= ~0x0020;//if( (stat & 0x0020) != 0 ){stat ^= 0x0020;}
		stat &= ~0x0040;//if( (stat & 0x0040) != 0 ){stat ^= 0x0040;}
		stat &= ~0x0080;//if( (stat & 0x0080) != 0 ){stat ^= 0x0080;}
		stat |= 0x0000004;	//キーロック
		strdata[5] = "v=D";	//バージョン 13
		if(mode == 6){//板のURLを取得
			strdata[5] = strdata[5] + "&m=U&b=" + brd;
			//StringBuffer sbuf = new StringBuffer("v=D");
			//sbuf.append("&m=U&b=");
			//sbuf.append(brd);
		} else if(mode == 7){//書き込み
			String message;
			//message = bboard.getString();//.replace( ',', '\n');
			//bodytext = message;
			/*while(true){
				i = message.indexOf("\\,");	if(i < 0){break;}
				message = message.substring(0,i) + "%2C" + message.substring(i+2);
			}*/
			/*while(true){
				i = message.indexOf('%');	if(i < 0){break;}
				message = message.substring(0,i) + "%25" + message.substring(i+1);
			}
			while(true){
				i = message.indexOf('&');	if(i < 0){break;}
				message = message.substring(0,i) + "%26" + message.substring(i+1);
			}
			while(true){
				i = message.indexOf('=');	if(i < 0){break;}
				message = message.substring(0,i) + "%3D" + message.substring(i+1);
			}
			while(true){
				i = message.indexOf('+');	if(i < 0){break;}
				message = message.substring(0,i) + "%2B" + message.substring(i+1);
			}*/
			//message = message.replace( ',', '\n');
			message = com.j_phone.io.URLEncoder.encode(bboard.getString());
			name = btitle.getString();
			mail = bres.getString();
			//書き込みデータ作成
			if(strdata[0].indexOf("kakiko.com") != -1){
				strdata[5] = "key=" + th + "&FROM=" + com.j_phone.io.URLEncoder.encode(name) + "&mail=" + com.j_phone.io.URLEncoder.encode(mail) + "&MESSAGE=" + message + "&submit=%8f%91%82%ab%8d%9e%82%de&bbs=" + bbsname + "&time=" + th/*System.currentTimeMillis()/1000*/  + "&get=1&MIRV=kakkoii";
				//sbuf.append("key=" + th + "&FROM=" + name + "&mail=" + mail + "&MESSAGE=" + message + "&submit=書き込み&bbs=" + bbsname + "&time=" + th/*System.currentTimeMillis()/1000*/  + "&get=1&MIRV=kakkoii");
				//StringBuffer sbuf = new StringBuffer("key=" + th + "&FROM=" + name + "&mail=" + mail + "&MESSAGE=" + message + "&submit=書き込み&bbs=" + bbsname + "&time=" + th/*System.currentTimeMillis()/1000*/  + "&get=1&MIRV=kakkoii");
			}else if(strdata[0].indexOf("vip2ch.com") != -1){
				strdata[5] = "key=" + th + "&FROM=" + com.j_phone.io.URLEncoder.encode(name) + "&mail=" + com.j_phone.io.URLEncoder.encode(mail) + "&MESSAGE=" + message + "&bbs=" + bbsname + "&time=" + th + "&suka=pontan&submit=%8F%E3%8BL%91S%82%C4%82%F0%8F%B3%91%F8%82%B5%82%C4%8F%91%82%AB%8D%9E%82%DE";
//				sbuf.append("key=" + th + "&FROM=" + name + "&mail=" + mail + "&MESSAGE=" + message + "&submit=書&bbs=" + bbsname + "&time=" + th + "&get=1&hana=mogera");
				//StringBuffer sbuf = new StringBuffer("key=" + th + "&FROM=" + name + "&mail=" + mail + "&MESSAGE=" + message + "&submit=書&bbs=" + bbsname + "&time=" + th + "&get=1&hana=mogera");
			}else{
				strdata[5] = "key=" + th + "&FROM=" + com.j_phone.io.URLEncoder.encode(name) + "&mail=" + com.j_phone.io.URLEncoder.encode(mail) + "&MESSAGE=" + message + "&submit=%8f%91%82%ab%8d%9e%82%de&bbs=" + bbsname + "&time=" + th + "&get=1";
//				sbuf = "key=" + th + "&FROM=" + name + "&mail=" + mail + "&MESSAGE=" + message + "&submit=書&bbs=" + bbsname + "&time=" + th + "&get=1");
				//StringBuffer sbuf = new StringBuffer("key=" + th + "&FROM=" + name + "&mail=" + mail + "&MESSAGE=" + message + "&submit=書&bbs=" + bbsname + "&time=" + th + "&get=1");
			}
		} else {// if(mode != 0){
			StringBuffer sbuf = new StringBuffer("v=D");
			if(mode == 0 || mode == 3){
				data[78] = 0x00000008;	//一般のダウンロード
			} else {
				Bugln("ThreadView...");
				data[78] = 0x00000002;	//スレッドリスト
				if(mode == 5){
					data[78] = 0x00000004;	//サーバーが選択
					//strdata[5] = strdata[5] + "&m=u&u=" + strdata[6] + "&c=b" + data[0] + "r" + data[1];
					sbuf.append("&m=u&u=");
					sbuf.append(strdata[6]);
					sbuf.append("&c=b");
					sbuf.append(data[0]);
					sbuf.append("r");
					sbuf.append(data[1]);
					//stat3 |= 0x0000004;
				} else if(mode != 4){
					//strdata[5] = strdata[5] + "&b=" + brd;
					sbuf.append("&b=");
					sbuf.append(brd);
					if(to == 0){
						if(mode == 1){to = st + data[0] - 1;}
						if(mode == 2 && st > 0){to = st + data[1] - 1;}
					}

					if(st > 0 && ! (st == 1 && to == 10)){
						//strdata[5] = strdata[5] + "&c=s" + st + "t" + to;
						sbuf.append("&c=s");
						sbuf.append(st);
						sbuf.append("t");
						sbuf.append(to);
					} else if(st < 0){//最新レス
						if(to == 0){to = data[1];}
						//strdata[5] = strdata[5] + "&c=l" + to;
						sbuf.append("&c=l");
						sbuf.append(to);
					}
				} else {	//スレ検索
					Bugln("ThreadSearch...");
					//strdata[5] = strdata[5] + "&m=s&b=" + brd + "&w=" + strdata[6];
					sbuf.append("&m=s&b=");
					sbuf.append(brd);
					sbuf.append("&w=");
					sbuf.append(strdata[6]);
				}
				if(mode == 2){	//レス表示の時のみ
					Bugln("ResView...");
					data[78] = 0x00000001;	//レス
					//strdata[5] = strdata[5] + "&t=" + th;
					sbuf.append("&t=");
					sbuf.append(th);
				}
			}
			//if(mode == 1 || mode == 2 || mode == 4 || mode == 5 || mode == 6 || mode == 7){
			//if(mode != 7){
			if(data[76] == 1){sbuf.append("&p=p1");}//strdata[5] = strdata[5] + "&p=p1";}	//非可逆のみ
			else {sbuf.append("&p=p3");}//strdata[5] = strdata[5] + "&p=p3";}	//圧縮なし



			if(data[80] == 6){	//gzip
				//strdata[5] = strdata[5] + "sd";
				sbuf.append("sd");
				data[78] |= 0x00000100;	//gzip圧縮指定
			} else if(data[80] != 0) {
				//strdata[5] = strdata[5] + "sd" + data[80];
				sbuf.append("sd");
				sbuf.append(data[80]);
				data[78] |= 0x00000100;	//gzip圧縮指定
			}


			//}
			if(mode == 2 || mode == 5){	//レス表示の時のみ
				if((data[49] & 0x04) == 0){sbuf.append("i");}//strdata[5] = strdata[5] + "i";}
				if((data[49] & 0x08) == 0){sbuf.append("t");}//strdata[5] = strdata[5] + "t";}
				if((data[49] & 0x10) == 0){sbuf.append("m");}//strdata[5] = strdata[5] + "m";}
				if((data[49] & 0x40) != 0){sbuf.append("n");}//strdata[5] = strdata[5] + "n";}

				i = data[87] & 0xFF;
				if(i == 2){sbuf.append("a");//strdata[5] = strdata[5] + "a";
				} else if(i > 2){sbuf.append("a" + i);}//strdata[5] = strdata[5] + "a" + i;}
				//if((data[57] & 0x00000002) == 0){strdata[5] = strdata[5] + "a";}

				i = (data[87] & 0xFF00) >> 8;
				if(i == 2){sbuf.append("u");//strdata[5] = strdata[5] + "u";
				} else if(i > 2){sbuf.append("u" + i);}//strdata[5] = strdata[5] + "u" + i;}
				//if((data[57] & 0x00004000) != 0){strdata[5] = strdata[5] + "u2";
				//} else if((data[57] & 0x00000004) == 0){strdata[5] = strdata[5] + "u";}

				if((data[57] & 0x00002000) != 0){sbuf.append("r");}//strdata[5] = strdata[5] + "r";}	//透明削除対策
			}
			//拡張オプション
			//strdata[5] = strdata[5] + extendedoption;
			sbuf.append(extendedoption);
			strdata[5]=sbuf.toString();
			sbuf = null;
		}

//#ifdef DEBUG
//		System.out.println(server + "2.cgi?"+strdata[5]);
//#endif

		//data[82] = data[82] * 10;
		///System.out.println("1/3倍化:" + data[82]);
		if(mode == 0){
//#ifdef DOJA	//DOJA
			stat3 |= 0x0100000;	//板一覧の取得
			stat |= 0x0010;	//通信
			//thttpget();
//#endif
		} else if(mode == 6 || mode == 7) {
			data[78] = 0x00000208;	//一般のダウンロード,iMonaヘッダ無し
			if(mode == 6){
				Bugln("Other...");
				//data[78] = 4;	//一般のダウンロード(ヘッダ無し)

				data[79] = brd;
				strdata[1] = null;
			} else if(mode == 7){//書き込み
				Bugln("Writing...\n");
				data[78] |= 0x00000010;	//書き込み
				//data[78] = 5;	//書き込み
			}/* else if(mode == 13){//AAS
				data[79] = brd;
				strdata[1] = null;
				data[72] = 1;
			}*/
			Thread thread = new Thread() {
				public final void run() {
					//stat |= 0x0001000;	//板一覧取得中
					stat |= 0x0010;	//通信
					httpgetdata();
					if((stat2 & 0x0001000) == 0){	//エラーメッセージが出ていないとき
						//strdata[7] = new String(dlarray);
						//stat2 |= 0x0001000;
						if((data[78] & 0x00000010) != 0){//書き込み
							Bugln("Ok!\n");
							stat2 |= 0x0001000;
							if((new String(dlarray)).indexOf("書きこみました") != -1 || (new String(dlarray)).indexOf("<!-- 2ch_X:true -->") != -1 || (new String(dlarray)).indexOf("<!-- 2ch_X:false -->") != -1){
								strdata[7] = "書込完了";
								Bugln("Writing=Complete!\n");
								bodytext = "";
								//addNamelist(btitle.toString());
							} else if((new String(dlarray)).indexOf("規約") != -1){
								strdata[7] = "書込失敗(規約)";
								Bugln("Writing=Kiyaku!\n");
								Bugln(new String(dlarray),1);
								viewwritepanel();
							} else if((new String(dlarray)).indexOf("ＥＲＲＯＲ") != -1) {
								String errortext;
								int start, end;
								errortext = new String(dlarray);
								//<b>ＥＲＲＯＲ：本文がありません！</b>
								start = errortext.indexOf("<b>") + 3 + 12;
								end  = errortext.indexOf("</b>");
								strdata[7] = "書込失敗\n" + errortext.substring(start,end);
								Bugln("Writing=Error!\n");
								Bugln(errortext + "\n");
								Bugln(new String(dlarray),1);
								//strdata[7] = "書込失敗:" + errortext.substring(start,end);
								viewwritepanel();
							} else {
								Bugln("Writing=UnknownError!\n");
								strdata[7] = "原因不明の書込失敗";
								try{
									/*String buf;
									buf = new String(dlarray);
									tbox = new LocalizedTextBox("原因不明な書込失敗", buf.toString(), buf.length(), LocalizedTextField.ANY);
									tbox.addCommand(command[2]);
									stat3 |= 0x0002000;
									tbox.setCommandListener(parent.canv);
									disp.setCurrent(tbox);*/
									Bugln(new String(dlarray),1);
								} catch(Exception e){
								}
								//viewwritepanel();
							}
							//stat2 |= 0x0001000;
						}/* else if(data[72] == 1){ //AAS
							strdata[1] = new String(dlarray);
							strdata[0] = strdata[1];
							if(strdata[0].charAt(strdata[0].length() - 1) == '/'){
								strdata[0] = strdata[0].substring( 0, strdata[0].length() - 1);
							}
							int i = strdata[0].lastIndexOf( '/', strdata[0].length() - 1);
							bbsname = strdata[0].substring(i + 1);
							strdata[0] = strdata[0].substring( 0, i);
							String buf = "example.ddo.jp/aas/a.i/" + strdata[0] + "/" + bbsname + "/" + nCacheTh[nCacheIndex][0] + "/" + (data[6] + nCacheSt[nCacheIndex]);
							data[72] = 0;
							openbrowser(buf, 1);
							data[78] &= ~0x00000300;
						}*/else {//if(data[78] == 4){
							strdata[1] = new String(dlarray);
							if((stat4 & 0x0040000) != 0){
								stat4 ^= 0x0040000;
								viewwritepanel();
								Bugln("WritingUrlGet\n");
							}else{
								Bugln("AASUrlGet\n");
								String buf = "";
								strdata[0] = strdata[1];
								if(strdata[0].charAt(strdata[0].length() - 1) == '/'){
									strdata[0] = strdata[0].substring( 0, strdata[0].length() - 1);
								}
								int it = strdata[0].lastIndexOf( '/', strdata[0].length() - 1);
								bbsname = strdata[0].substring(it + 1);

/*								if((data[78] & 0x0000800) != 0){//c.2ch
									data[78] &= ~0x0000800;
									buf = "c.2ch.net/test/-/" + bbsname + "/" + nCacheTh[nCacheIndex][0] + "/" + (data[6] + nCacheSt[nCacheIndex]) + "-";
								}else/* if((data[78] & 0x0000800) != 0)*///{//AAS*/
									//data[78] &= ~0x0000400;
									strdata[0] = strdata[0].substring( 0, it);
									buf = "example.ddo.jp/aas/a.i/" + strdata[0] + "/" + bbsname + "/" + nCacheTh[nCacheIndex][0] + "/" + (data[6] + nCacheSt[nCacheIndex]);

								//}
								openbrowser(buf,1);
							}
						}

					}
					stat |= 0x1000000;	//画面更新
					//stat &= ~0x0001000;	//板一覧取得中
				}
			};
			thread.start();
		} else {

			Linkref[0] = brd;
			Linkref[1] = th;
			Linkref[2] = st;
			//Linkref[0] = to
			strdata[10] = strdata[8];
//#ifdef DEBUG
//			System.out.println(brd + " " + th + " " + st);
//#endif
			stat |= 0x0010;	//通信
			stat &= ~0x40020000;	//Loading中の操作は不可能(0x40000000)、Loading中のスレ読みを解除
			if((stat3 & 0x0000080) != 0){//先読みをすることを示すフラグ
				stat3 &= ~0x0000080;	//先読みをすることを示すフラグ
				stat |= 0x0020000;	//Loading中のスレ読み
			}
		}
//		System.out.println(server + "2.cgi?"+strdata[5]);
		stat2 &= ~0x0004000;	//function解除
		stat |= 0x1000000;	//画面更新
	}

	/**
	 * 書込画面を表示
	 */
	public final void viewwritepanel(){
		int i;
		//String[] namedata = new String[10];
		strdata[0] = strdata[1];
		if(strdata[0].charAt(strdata[0].length() - 1) == '/'){
			strdata[0] = strdata[0].substring( 0, strdata[0].length() - 1);
		}
		i = strdata[0].lastIndexOf( '/', strdata[0].length() - 1);
		bbsname = strdata[0].substring(i + 1);
		strdata[0] = strdata[0].substring( 0, i);
		Bugln("*WritingUrlState\n" + "strdata[0]:" + strdata[0] + "\nbbsname:" + bbsname + "\ni:" + i + "\n");
		btitle = new LocalizedTextField("名前",name,1024,LocalizedTextField.ANY);
		bres = new LocalizedTextField("E-mail",mail,1024,LocalizedTextField.ANY);
		bboard = new LocalizedTextField("内容",bodytext,4096,LocalizedTextField.ANY);
//		bthread = new LocalizedTextField("ｽﾚ番号(変更不要)",""+BookMarkData[i * 3 + 1],16,LocalizedTextField.NUMERIC);
		inputForm = new Form("書き込み");
		inputForm.append(btitle);
		inputForm.append(bres);
		inputForm.append(bboard);
		//inputForm.append(new StringItem("「,」で改行することができます",""));
		if( (stat & 0x40000) != 0 ){	//ﾚｽ見てる時
			inputForm.append(new LocalizedTextField("URL等",">>" + (data[6] + nCacheSt[nCacheIndex]) + "\n" + strdata[9] + "\nhttp://" + strdata[0] + "/test/read.cgi/" + bbsname + "/" + nCacheTh[nCacheIndex][0] + "/" + "\nhttp://c.2ch.net/test/-/" + bbsname + "/" + nCacheTh[nCacheIndex][0] + "/i",500,LocalizedTextField.ANY));
		} else if((stat2 & 0x0010000) != 0){	//ブックマーク
			inputForm.append(new LocalizedTextField("URL等",tBookMark[(data[67] + data[68])] + "\nhttp://" + strdata[0] + "/test/read.cgi/" + bbsname + "/" + BookMarkData[(data[67] + data[68]) * 3 + 1] + "/" + "\nhttp://c.2ch.net/test/-/" + bbsname + "/" + BookMarkData[(data[67] + data[68]) * 3 + 1] + "/i",500,LocalizedTextField.ANY));
		}

		//namedata = loadNamelist(0);
		//inputForm.append(new LocalizedTextField("名前履歴",namedata[0],4096,LocalizedTextField.ANY));
//		inputForm.append(bthread);
		inputForm.addCommand(command[8]);
		inputForm.addCommand(command[2]);
		inputForm.setCommandListener(this);
		disp.setCurrent(inputForm);
		stat2 |= 0x20000000;
	}
//	public final void write(){
//	}
	//SO SC
	//1  1  > OFF
	//1  0  > OFF
	//0  1  > ON
	//0  0  > OFF

	/**
	 * スクロール処理
	 */
	public final void Scroll(){
//		int i = data[60] + data[65];
		if( (stat & 0x0300000) != 0 ){//上下スクロールON
			int k = 0, n = 0, m, i;
			data[95] = 0;
			//stat3 |= 0x0010000;	//スクロール実績
			if(data[60] != -1){
				k = data[30] + data[34];//高さ+行間
				n = data[77] / k;//	if(0 > n){n = 0;}	//DivStrの書き始め
				k = (height + data[77] - data[30] - 4) / k + 1;	//これ以上は表示されないので書かない
				//if(k > data[85]){k = data[85];}	//indexからはずれないために
			}
			if(data[59] > 0){
				i = (Linklist[data[60]] / 1000) % 10000;	//現在フォーカスが当たっているリンクの行
			} else {i = k;}
			//System.out.println("i:" + i + " n:" + n + " k:" + k + " 88:" + data[88]);
			//i:現在フォーカスが当たっているリンクの行
			//m:一つ前or次のリンクの行
			//n:書き始め
			//k:これ以上は表示されないので書かない

			if( (stat & 0x0100000) != 0 ){//上スクロールON
	//			if(data[65] > 0){	//Linkの選択スキップ分
	//				data[65]--;
	//				stat |= 0x1000000;	//画面更新
				if(/*data[60] != -1 && */data[60] > 0){
					m = (Linklist[data[60]-1] / 1000) % 10000;
					if(data[88] == 0 || i > k){
						if( ((data[57] & 0x00028000) != 0 && (stat & 0x0010000) != 0) ||
						 (/*m == 0 || */m > n || (data[77] == 0 && (stat & 0x0040000) == 0) || (data[77] == -data[30] && (stat & 0x0040000) != 0))/* || m <= k*/){	//リンク張るところは表示内にある場合
							data[60]--;	i = m;
							data[88] = -3;
							//stat |= 0x1000000;	//画面更新
							//return;
						}
					} else if(data[88] > 0) {
						data[88] = -3;
					} else {
						data[88]++;
					}
				}
				if((data[57] & 0x00028000) != 0 && (stat & 0x0010000) != 0){
					if((data[57] & 0x00008000) != 0 || i < n){
						data[77] = i/*((Linklist[data[60]] / 1000) % 10000)*/ * (data[30] + data[34]);
					}
				} else if((data[77] > 0 || (data[77] > -data[30] && (stat & 0x0040000) != 0)) ){
					if( (data[49] & 0x20) != 0 || (data[49] & 0x01) == 0 ){stat ^= 0x0100000;}
					data[77] -= data[35];
					if((stat & 0x0040000) != 0){
						if(data[77] < -data[30]){data[77] = -data[30];}
					} else if(data[77] < 0){data[77] = 0;}
	/*				if(i > 1 && (Linklist[i] / 1000) % 10000 == (Linklist[i - 1] / 1000) % 10000){
						data[65]--;
					} else {
						data[65] = 0;
					}
	*/
					//repaint();	continue;//高速化
				}
				//stat |= 0x1000000;	//画面更新
			} else if( (stat & 0x0200000) != 0 ){//下スクロールON
			//System.out.println("i:" + i + " " + data[59] + " " + data[65] + " " + data[60]);
	//			if(data[60] != -1 && i + 1 < data[59] && (Linklist[i] / 1000) % 10000 == (Linklist[i + 1] / 1000) % 10000){
	//				data[65]++;
	//				stat |= 0x1000000;	//画面更新
				if(data[60] != -1 && data[60] + 1 < data[59]){
					m = (Linklist[data[60]+1] / 1000) % 10000;
					if(data[88] == 0 || i < n){
						if(/*(m == 0 || m > n) &&*/ m <= k-5/*k-1*/ || ((data[57] & 0x00028000) != 0 && (stat & 0x0010000) != 0)){	//リンク張るところは表示内にある場合
							data[60]++;	i = m;
							data[88] = 3;
							//stat |= 0x1000000;	//画面更新
							//return;
						}
						//m = (Linklist[data[60]] / 1000) % 10000;
						//if(m <= k-3){
						//	if(m > n/* || m <= k*/){	//リンク張るところは表示内にある場合
						//		if(m == (Linklist[data[60] + 1] / 1000) % 10000){
						//			data[60]++;
						//			//stat |= 0x1000000;	//画面更新
						//			//return;
						//		}
						//	} else {
						//		data[60]++;
						//	}
						//}
					} else if(data[88] < 0) {
						data[88] = 3;
					} else {
						data[88]--;
					}
				}
				if((data[57] & 0x00028000) != 0 && (stat & 0x0010000) != 0){
					if(data[60] + 1 < data[59]){
						m = (Linklist[data[60]+1] / 1000) % 10000;
					} else {
						m = i + 3;
					}
					if((data[57] & 0x00008000) != 0 || m > k-3){	//スレごとのスクロール
						data[77] = i/*((Linklist[data[60]] / 1000) % 10000)*/ * (data[30] + data[34]);
					}
				} else if((data[77] < (data[85] + 5/*2*/) * (data[30] + data[34]) - height)){
	//				data[65] = 0;
					if( (data[49] & 0x20) != 0 || (data[49] & 0x01) == 0 ){stat ^= 0x0200000;}
					//if((stat & 0x0010000) != 0 && data[35] > data[30] + data[34]){	//スレ選択中 & スクロール量が文字の高さを超えるとき
					//	data[77] += data[30] + data[34];
					//} else {
						data[77] += data[35];
					//}
					//stat |= 0x1000000;	//画面更新
					//repaint();	continue;//高速化
	//			} else if((data[60]/* + data[65]*/) < data[59]-1){	//Linkの選択スキップ分
	//				data[65]++;
	//				data[60]++;
	//				stat |= 0x1000000;	//画面更新
				}
			}
			stat |= 0x1000000;	//画面更新
		} else if( (stat2 & 0x0400000) != 0 ){//左スクロールON
			data[95] = 0;
			//stat3 |= 0x0010000;	//スクロール実績
			if(data[48] < 0){
				if( (data[49] & 0x01) == 0 ){stat2 ^= 0x0400000;}
				data[48] += data[35];
				if(data[48] > 0){
					data[48] = 0;
				}
				stat |= 0x1000000;	//画面更新
			}
		} else if( (stat2 & 0x0800000) != 0 ){//右スクロールON
			data[95] = 0;
			//stat3 |= 0x0010000;	//スクロール実績
			if( (data[49] & 0x01) == 0 ){stat2 ^= 0x0800000;}
			data[48] -= data[35];
			stat |= 0x1000000;	//画面更新
		}
	}

	/**
	 * 待機処理
	 * @param w ミリ秒で待機時間
	 */
	public final void wait(int w){
		try{Thread.sleep(w);}catch (Exception e){}
	}

	/**
	 * ran。いわゆる最初に実行されるメソッドでここの中をぐるぐる回る
	 */
	public final void run(){
		//int s = 200;
		while(true){
//			try{
			//if(((stat & 0x0010) != 0 || (stat & 0x10000000) != 0) && (stat & 0x0300000) == 0/*scroll*/ && (stat & 0x1000000) == 0){	//通信中or通信後の処理中
			if((stat & 0x10000010) != 0 && (stat & 0x1300000) == 0){	//通信中or通信後の処理中
//高速な端末ではスリープ時間を短くして、スクロールの反応速度を上げる

				wait(data[82] * 10);	//10倍スリープ
				if(data[95] >= 0){
					data[95] += data[82];
				}
				stat |= 0x2000000;	//トップのみ更新
			} else {
				wait(data[82]);
				if(data[95] >= 0){
					data[95] += data[82];
				}
			}
//			}catch (Exception e){
//			}

			//5秒以上操作がないときは電池、電波情報を表示する。
			if(data[97] != 0){
				if(data[95] > data[97]){
					data[95] = -1;
					stat |= 0x1000000;	//画面更新
				}
			}
			//if( (stat & 0x10000000) != 0 ){	//通信後の処理中(通信は完了)
			//	stat |= 0x2000000;	//トップのみ更新
			//}
			if( (stat & 0x0450000) != 0/* && (stat & 0xF000000) == 0*/){	//レス表示中
				Scroll();	//ﾚｽｽｸﾛｰﾙ
			}

			if( (stat & 0xF000000) != 0){	//再描画
				repaint();
			}
			if( (data[49] & 0x02) != 0){	//秒表示
				repaint();
			}
		}
	}
	/**
	 * 実際の通信処理
	 * mode(data[78]) - 1:レスの取得(3:+タイトル) 2:スレ一覧の取得(4:+タイトル)
	 * @see MainCanvas#httpinit
	 * @see MainCanvas#thttpget
	 */
	public final void httpgetdata(){
		int i, j = 0, k = 0, n = 0;
		//int mode = 0;//1:レスの取得(3:+タイトル) 2:スレ一覧の取得(4:+タイトル)
		//int t1 = 0, t2 = 0;//テンポラリ t1:スレ番号 t2:板番号

		int rc = 0;	//response code

		while( (stat & 0x10000000) != 0 ){}
		//System.out.println("http");
		if( (stat & 0x0010) == 0 ){	//通信中ではない
			return;
		}
		int mode = data[78];

		//String url = new String("http://localhost:81/i2ch/2.cgi");
		//HttpConnection co = null;
		InputStream in = null;
//#ifdef DOJA505
//		JarInflater jarinf;
//#endif
		int errorcode = 0;	//通信のエラーコード
		//InputStreamからByteArrayOutputStreamへ入れ替え
		
		ByteArrayOutputStream outstr = new ByteArrayOutputStream();
		try {
			byte d1[] = new byte[1];
			//int len;
			data[14] = 0;	data[15] = 0;	data[16] = 0;	data[17] = 0;	data[18] = 0;
			///System.out.println("CON:START!");
			//通信定義
			//HttpConnection co = (HttpConnection)Connector.open(IApplication.getCurrentApp().getSourceURL() + "2.cgi",Connector.READ_WRITE,true);
			//HttpConnection co = (HttpConnection)Connector.open("http://localhost:81/i2ch/2.cgi",Connector.READ_WRITE,true);

			if((mode & 0x00000010) != 0){	//書き込み
				Bugln("WritingUrl:" + "http://" + strdata[0] + "/test/bbs.cgi\n");
				co = (HttpConnection)Connector.open("http://" + strdata[0] + "/test/bbs.cgi",Connector.READ_WRITE,true);
			} else {
				co = (HttpConnection)Connector.open(server + "2.cgi",Connector.READ_WRITE,true);
			}
//#ifdef DEBUG
//				System.out.println("CONNECTION:OPEN");
//#endif
			co.setRequestMethod(HttpConnection.POST);

			//HTTPヘッダを書き込み
			if((mode & 0x00000010) != 0){	//書き込み
				if( (stat & 0x40000) != 0 ){	//ﾚｽ見てる時
					Bugln("WritingReferer:" + "http://" + strdata[0] + "/test/read.cgi/" + bbsname + "/" + nCacheTh[nCacheIndex][0] + "/\n");
					co.setRequestProperty("Referer",
							"http://" + strdata[0] + "/test/read.cgi/" + bbsname + "/" + new Integer(nCacheTh[nCacheIndex][0]).toString() + "/");
				} else if((stat2 & 0x0010000) != 0){	//ブックマーク
					Bugln("WritingReferer:" + "http://" + strdata[0] + "/test/read.cgi/" + bbsname + "/" + BookMarkData[(data[67] + data[68]) * 3 + 1] + "/\n");
					co.setRequestProperty("Referer",
							 "http://" + strdata[0] + "/test/read.cgi/" + bbsname + "/" + new Integer(BookMarkData[(data[67] + data[68]) * 3 + 1]).toString() + "/");
			
				}
				co.setRequestProperty("User-Agent",
						"Monazilla/1.00 iMona-zuzu/1.0.x");
				
				
				//Cookie: NAME=名前; MAIL=メール; SPID(PON)=値; expires=有効期限; path=/
				co.setRequestProperty("Cookie", 
						"NAME=" + com.j_phone.io.URLEncoder.encode(name) + "; MAIL=" + com.j_phone.io.URLEncoder.encode(mail) + "; " + com.j_phone.io.URLEncoder.encode(cookie));
				Bugln("NAME=" + com.j_phone.io.URLEncoder.encode(name) + "; MAIL=" + com.j_phone.io.URLEncoder.encode(mail) + "; " + com.j_phone.io.URLEncoder.encode(cookie) + "\n");

				/*if(strdata[0].indexOf("vip2ch.com") != -1){

				}*/
			}
			//data[16] = 0;
			//OutputStreamへ送るデータを入れる
			OutputStream out = co.openOutputStream();
			out.write(strdata[5].getBytes());
			out.close();
			stat |= 0x0020;	//接続準備	httpstat = 1;
			if((stat & 0x0080000) != 0){//通信ストップ要求
				throw new Exception();
			}
//#ifdef
			rc = co.getResponseCode();
			if(co.getHeaderField("set-cookie") != null){
				cookie = co.getHeaderField("set-cookie");
				//Bugln(co.getHeaderField("set-cookie") + "\n");
			}
			
//#endif
			errorcode++;	//通信のエラーコード1
			if((stat & 0x0080000) != 0 || rc >= 300){//通信ストップ要求orレスポンスコードが300以上
				throw new Exception();
			}
			//InputStreamへレスを入れる
			in = co.openInputStream();
			stat |= 0x0040;	//接続完了＆受信中	httpstat = 2;
//#ifdef DEBUG
//				System.out.println("CONNECTION:INPUTOPEN");
//#endif
			errorcode++;	//通信のエラーコード2
			System.gc();	//ガベージコレクション

			if(data[80] != 0 && (mode & 0x00000100) != 0/*(data[78] == 0 || data[78] == 1 || data[78] == 2)*/){	//gzip圧縮指定がある場合
				in.read(d1);
				if(d1[0] == 0x01){
					i = 1;
					while(true){	//容量の取得
						if(in.read(d1) < 0){break;}
						i++;
						if(d1[0] == 0x0A){//\n
							data[17] = to10(outstr.toByteArray()) + i;
							outstr.reset();
							break;
						} else {
							outstr.write(d1);
						}
					}
					//P4,P5の機能を使用する
					in = new InflateInputStream(in);
				}
			}
			errorcode++;	//通信のエラーコード3
			if((mode & 0x00000200) == 0/*data[78] != 4 && data[78] != 5*/){	//ヘッダ付きの通信の場合

				if(in.read(d1) < 0){
					throw new Exception();
				}
				if((mode & 0x00000004) != 0/*data[78] == 0*/){	//サーバー側で、スレッドの読み込みか板の読み込みかを決める場合
					mode ^= 0x00000004;
					//if(d1[0] == 0x01 || d1[0] == 0x03){	//レス
					//	mode = 1;
					i = 0;
					//if(d1[0] == 0x01 || d1[0] == 0x03){	//レス
					if(d1[0] == 0x13){	//レス
						mode |= 0x00000001;
						//stat &= ~0x0040000;	//レス表示中のときは解除
						//stat3 |= 0x0000080;	//板一覧が無効(再読込が必要)
					//} else if(d1[0] == 0x02 || d1[0] == 0x04){	//スレッドリスト
					} else if(d1[0] == 0x14){	//スレッドリスト
						mode |= 0x00000002;
					}
					//この二つ以外はエラー
					//stat |= 0x0020000;	//レス表示中(+レス続き取得中)
					if((mode & 0x00000003) != 0){	//エラーでない場合
						while(true){
							if(in.read(d1) < 0){break;}
							if(d1[0] == 0x0A){
								strdata[10] = outstr.toString();	//スレッドタイトル
								if(strdata[10].length() == 0){
									strdata[10] = "?";
								}
//#ifdef DEBUG
//								System.out.println("title:" + strdata[8]);
//#endif
								break;
							} else if(d1[0] == '\t'){
								if(i == 0){
									Linkref[0] = to10(outstr.toByteArray());	//板番号
								} else {
									Linkref[1] = to10(outstr.toByteArray());	//スレナンバー
								}
								i++;
								outstr.reset();
							} else {
								outstr.write(d1);
							}
						}
						in.read(d1);
					}
	/*
					} else if(d1[0] == 0x02 || d1[0] == 0x04){	//スレッドリスト
					//	mode = 2;
						//stat |= 0x8000;	//スレ一覧ＤＬ中
						data[78] = 2;
						while(true){
							if(in.read(d1) < 0){break;}
							if(d1[0] == 0x0A){
								strdata[3] = outstr.toString();	//板名
								break;
							} else if(d1[0] == '\t'){
								Linkref[0] = to10(outstr.toByteArray());	//板番号
								outstr.reset();
							} else {
								outstr.write(d1);
							}
						}
					}
	*/
				}
				outstr.reset();
//#ifdef
//				errorcode++;	//通信のエラーコード4
//#endif
				k = 1;	data[15]++;
				if(0 <= d1[0] && d1[0] <= 0x0F){
					if(d1[0] == 0x00/*'0'*/){
						strdata[7] = "なんらかのｴﾗｰ";//StrList[10][5];
					} else if( d1[0] == 0x01/*'A'*/ ){	//新レスがない
						strdata[7] = "新ﾚｽはありません";//StrList[10][4] + StrList[10][18];
						Bugln("Ok\n");
					} else if( d1[0] == 0x02/*'B'*/ ){	//spdv->2chへの接続エラー
						strdata[7] = "通信ｴﾗｰ(2ch)";
						Bugln("Ok\n");
					} else if( d1[0] == 0x03 ){	//検索で何も見つからなかったとき
						strdata[7] = "No hit...";
						Bugln("Ok\n");
					} else if( d1[0] == 0x04 ){	//DAT落ち
						strdata[7] = "DAT落ち";
						Bugln("Ok\n");
					} else if( d1[0] == 0x05 ){	//人多杉
						strdata[7] = "人多杉";
						Bugln("Ok\n");
					} else if( d1[0] == 0x06){//中間鯖移転&削除
						strdata[7] = "中間鯖が移転したみたい";
					} else {// if( 0x06 <= d1[0] && d1[0] <= 0x0F ){	//サポートされていないエラー
						strdata[7] = "その他のｴﾗｰ";//StrList[10][4] + StrList[10][18];
						Bugln("Ok\n");
					}
					stat2 |= 0x0001000;
				} else {
					Bugln("Ok\n");
					//キャッシュに追加
					if((mode & 0x00000003) != 0){
						//全てのキャッシュのTTLを1増やし、TTLが一番大きい又は無効であるキャッシュを選択する
						j = 0;	n = 0;
						i_length = CacheTitle.length;
						for(i = 0;i < i_length;i++){
							//nCacheTTL[i]++;
							if(j == -1){
							} else if(nCacheTTL[i] == -1){
								j = -1;	n = i;
							} else if(nCacheTTL[i] > j){
								j = nCacheTTL[i];	n = i;
							}
						}
						nCacheTTL[n] = -1;	//キャッシュを編集するためこのキャッシュを無効にする
						if((mode & 0x00000001) != 0){	//レスのダウンロード
							//CacheResData[n] = Res;
							CacheBrdData[n] = null;
							nCacheBrdData[n] = null;
							nCacheTh[n] = new int[1];	nCacheTh[n][0] = Linkref[1];
							//nCacheSt[n] = data[7];
							//nCacheTo[n] = data[8];
							//nCacheAll[n] = data[9];
						} else {//if(mode == 2){	//スレ一覧ダウンロード時
							searchtext = "";
							CacheResData[n] = null;
							//CacheBrdData[n] = ThreadName;
							//nCacheBrdData[n] = nRes;
							//nCacheTh[n] = nThread;
							//nCacheSt[n] = Linkref[2];
							//nCacheTo[n] = Linkref[2] + nThread.length - 1;
							nCacheAll[n] = 0;
						}
						nCacheBrd[n] = Linkref[0];
					}
					outstr.write(d1);
					i = 0;
					while(true){	//容量の取得
						if(in.read(d1) < 0){break;}
						k++;	data[15]++;
						if(d1[0] == '\t'){//\t
							j = to10(outstr.toByteArray());
							if( (mode & 0x00000001) != 0/*data[78] == 1*//*((stat & 0x20000) != 0 || (stat & 0x40000) != 0)*/){	//レスのダウンロード
								if(i == 0){	data[14] = j;	//len
								} else if(i == 1) {data[18] = j;	//lenbefore
								} else if(i == 2) {nCacheSt[n]/*data[7]*/ = j;	//from
								} else if(i == 3) {nCacheTo[n]/*data[8]*/ = j;	//to
								//} else if(i == 1) {data[9] = to10(outstr.toByteArray());	//all
								}	i++;
							} else {
								data[14] = j;	//len
							}

							outstr.reset();
						} else if(d1[0] == 0x0A){//\n
							j = to10(outstr.toByteArray());
							if( (mode & 0x00000001) != 0/*data[78] == 1*//*((stat & 0x20000) != 0 || (stat & 0x40000) != 0)*/){	//レスのダウンロード
								nCacheAll[n]/*data[9]*/ = j;	//all
								//for(j = 0;j < CacheTitle.length;j++){	//キャッシュ内の検索
								//	if(nCacheTTL[j] >= 0 && CacheBrdData[j] == null && nCacheTh[j] != null && nCacheTh[j][0] == nCacheTh[n][0] && nCacheBrd[j] == nCacheBrd[n]){
								//		nCacheAll[j] = nCacheAll[n];	//All数を最新のものに更新
								//	}
								//}
							} else {
								data[18] = j;	//lenbefore
							}
							outstr.close();
							data[14] += k;
							data[18] += k;
							data[16] = k;//data[14];
							break;
						} else {
							outstr.write(d1);
						}
					}
				}
				if(data[16] == 0){	//DL失敗
					throw new Exception();
				}
			}

			outstr = new ByteArrayOutputStream(data[18] + 50);
			errorcode++;	//通信のエラーコード4
			//rbyte = 0;

			//data[14] += data[15] + 1;	//全体の読み込むバイト数
			while(true){	//データの取得 & 単純置き換え圧縮 & ランレングス圧縮を展開
				if((stat & 0x0080000) != 0){//通信ストップ要求
					throw new Exception();
				}
				//len = in.read(d1);
				//if(len <= 0) break;
				if(in.read(d1) <= 0) break;
				data[15]++;
				outstr.write(d1[0]);
				data[16]++;
			}
			errorcode++;	//通信のエラーコード5
			//通信終了処理
			in.close();//読み出し終了
			co.close();//通信終了
			/*if((mode & 0x00000010) != 0){	//書き込み
				if(co.getHeaderField("Set-Cookie").indexOf("PON") != -1){
					
				}
			}*/
			///System.out.println("UP:1");
			stat |= 0x0080;	//受信完了	httpstat = 4;
		//} catch(UnsupportedEncodingException e) {

		} catch(Exception e) {
			allprintStackTrace(e);
			strdata[11] =  e.toString();
			try{
				if(in != null){in.close();}
				if(co != null){co.close();}
			

			} catch(Exception e2) {
			}
			//try{
			//if(in != null)
			//	in.close();
			//in = null;
			//} catch(Exception e2) {
			//}
			///System.out.println("通信エラー");
			//通信失敗
			//stat &= ~0x0020;//if( (stat & 0x0020) != 0 ){stat ^= 0x0020;}
			//stat &= ~0x0040;//if( (stat & 0x0040) != 0 ){stat ^= 0x0040;}
			stat &= ~0x0060;	//通信エラーにする
			//stat |= 0x0200;	//test
			//stat |= 0x0080;
		}

		//空きメモリ確保
		in = null;
		co = null;
		
		
		stat3 &= ~0x0000004;	//LinkON(URL)でジャンプしたことを示すフラグ
	//if((stat & 0x1000) != 0){	//板データダウンロード時
		//スクラッチパッドから板データを取得する
	//}
		//パケット代の計算
		//calccost();
		//int value = data[15];
		i = data[15];

		if(data[17] != 0){
			//value = data[17];
			i = data[17];
		}

		//if(value < 0)
		//	return;
		i += 500;	//パケット代の補正(ヘッダとか)
		if(data[69] != 0){	//パケット代の単価が設定されている場合
			data[46] = i * data[69] / 1280;
			data[47] += data[46];
			//return;
		} else {	//自動設定の場合


			// \0.3/packet
			data[46] = i * 30 / 128;
			data[47] += data[46];
		}
		try {

//			if( (stat & 0x00F0) != 0x00F0 && (stat3 & 0x0000200) != 0){	//DLERROR and 参照元を保存していた場合
//				data[64]--;	//直前に保存した参照元を破棄する
//			}
			if( (stat & 0x00F0) == 0x00F0 ){	//ＤＬ終了(成功)
				byte[] inbyte;
				zuzu[0] = 0; //レス番号指定を0にする。

				//スレ又はレスのダウンロード
				if((mode & 0x00000003) != 0){
					CacheTitle[n] = strdata[10];//strdata[8];
					//nCacheTTL[n] = 0;
					//nCacheIndex = n;
				}
				System.gc();	//ガベージコレクション

				if((mode & 0x00000001) != 0/*data[78] == 1*/){	//レスの(追加)ダウンロード
					CacheResData[n] = new byte[nCacheTo[n]/*data[8]*/ - nCacheSt[n]/*data[7]*/ + 1][];
					data[45] = 0;//レスをどこまで処理したか
				}
				if((mode & 0x00000200) == 0 && (mode & 0x00000001) != 0){//iMona用のヘッダ有り&レスのDL
					//ひらがな圧縮の展開・レス処理
					inbyte = outstr.toByteArray();	outstr.reset();
					for(i = 0;i < inbyte.length;i++){
						if(inbyte[i] == 0x0A){	//\n


							CacheResData[n][data[45]]/*Res[data[45]]*/ = outstr.toByteArray();	data[45]++;	//レスをどこまで処理したか
							outstr.reset();
							if(data[45] == 1 && (stat & 0x0020000) == 0){	//初めてのレスを処理しているときandバックグランド受信をしていない
								stat |= 0x40000000;	//Loading中の操作は不可能
								if((stat3 & 0x0000010) != 0){	//戻ってるとき
									data[6] = nCacheTo[n]/*data[8]*/ - nCacheSt[n]/*data[7]*/;
								} else {	//進んでいるときorレス表示中ではない場合
									data[6] = 0;
								}
							}
							if(data[45] == data[6] + 1 && (stat & 0x0010) != 0 && (stat & 0x0020000) == 0){	//レスを表示できるところまでいっていたらandバックグランド受信をしていない
								stat |= 0x40000000;	//Loading中の操作は不可能
								nCacheIndex = n;	//現在読んでいるキャッシュのインデックス
								//strdata[9] = strdata[8];	//タイトルを更新
								stat2 |= 0x80000000;	//makeResで初期化を行う
								makeRes();
								stat |= 0x10000000;	//通信後の処理中

								//終了処理
								stat &= ~0x0000014;	//通信解除andキーストップ解除
								//stat &= ~0x0000004;	//キーストップ解除
								//stat &= ~0x0010;	//通信解除
							}
						} else {
							outstr.write(inbyte[i]);
						}
					}
					inbyte = null;
				}
//					if( data[78] == 1 ){	//レスのダウンロード
//					} else if(data[78] == 2){	//スレ一覧ダウンロード時
				if((mode & 0x00000002) != 0/*data[78] == 2*/){	//スレ一覧ダウンロード時
					//スレ一覧の文字処理
					inbyte = outstr.toByteArray();
					outstr.reset();
					i = (inbyte[0] & 0xFF) - 16;
					CacheBrdData[n]/*ThreadName*/	= new String[i];
					nCacheBrdData[n]/*nRes*/		= new int[i];
					nCacheTh[n]/*nThread*/			= new int[i];
					byte[] bArray;

					nCacheSt[n] = Linkref[2];
					nCacheTo[n] = Linkref[2] + i - 1;
/*
*/
					j = 0;	k = 0;
					for(i = 1;i < inbyte.length;i++){
						if(inbyte[i] == 0x09){
							if(j == 0){
								nCacheTh[n][k]/*nThread[k]*/ = to10(outstr.toByteArray());	j = 1;
								data[16] += 6;
								outstr.reset();
							} else if(j == 1){
								CacheBrdData[n][k]/*ThreadName[k]*/ = new String( outstr.toByteArray() );
								outstr.reset();
							}
						} else if(inbyte[i] == 0x0A){
							bArray = outstr.toByteArray();
							nCacheBrdData[n][k]/*nRes[k]*/ = to10(bArray);
							data[16] += GetDigits(nCacheBrdData[n][k]/*nRes[k]*/) - bArray.length;
							outstr.reset();
							j = 0;	k++;
						} else {
							outstr.write(inbyte[i]);
						}
					}
					bArray = outstr.toByteArray();
					nCacheBrdData[n][k]/*nRes[k]*/ = to10(bArray);
					data[16] += GetDigits(nCacheBrdData[n][k]/*nRes[k]*/) - bArray.length;
					//スレ一覧に移行するための処理
					if((stat & 0x0020000) == 0){	//バックグランド受信をしていない
						stat |= 0x40000000;	//Loading中の操作は不可能
						if((stat & 0x10000) == 0){	//スレ選択中ではない場合(板選択中orブックマーク)
							//addCommand(command[9]);//登録
							//data[5] = nCacheSt[n] - 1;//Linkref[2] - 1;
							data[60] = 0;
							//data[4] = 0;
							data[77] = 0;
							//data[10] = 0;data[11] = 0;

							//stat ^= 0x8000;	///スレ一覧ＤＬ中解除
							//if((stat3 & 0x0000020) == 0){	//ブックマークから板にジャンプした
							//	data[5] = BookMarkData[(data[67] + data[68]) * 3 + 2];	//レス
							//}
						} else {
							if( (stat & 0x0008000) != 0 ){//スレ一覧のリロード
								//data[5] = nCacheSt[n] - 1;//data[5] - data[4];
								data[60] = 0;
								//data[4] = 0;
							} else {
								//if(data[4] == 0){data[5]--;data[4] = nThread.length-1;} else {data[5]++;data[4] = 0;}
								if((stat3 & 0x0000010) != 0){	//レスを最後から読むとき(戻ってるとき)
									//data[5]--;
									data[60] = nCacheTh[n].length-1;
									//data[4] = nCacheTh[n]/*nThread*/.length-1;
								} else {	//進んでいるとき
									//data[5]++;
									data[60] = 0;
									//data[4] = 0;
								}
							}
						}
						//nCacheTTL[n] = 0;	//キャッシュを有効にする
						nCacheIndex = n;	//現在読んでいるキャッシュのインデックス
						stat2 |= 0x40000000;	//makeTLで初期化を行う
						makeTL();
					}
					//stat3 &= ~0x0000080;	//板一覧が無効(再読込が必要)を解除。

				} else if((mode & 0x00000018) != 0/*data[78] == 3 || data[78] == 4 || data[78] == 5*/){
					dlarray = outstr.toByteArray();
				} else if((mode & 0x00000001) != 0){
					data[90] += nCacheTo[n] - nCacheSt[n] + 1;
				}
				outstr.close();
				outstr = null;

				stat &= ~0x00E0;	//ＤＬ終了
				if( (stat & 0x0020000) != 0 ){	//バックグランド受信をしているとき
					nCacheTTL[n] = 0;	//キャッシュを有効にする
					chkcache();	//キャッシュチェック
				//} else if( (stat3 & 0x0000040) != 0){//「戻る」で戻っているとき
				//	data[64]--;
				}
			} else {	//DL ERROR
				if((stat3 & 0x0000200) != 0){//参照元を保存していた場合
					if(data[64] > 0){data[64]--;}	//失敗したときは直前に保存した参照元を破棄する
				}
				if((stat & 0x0080000) != 0){//通信ストップ要求
					strdata[7] = "中断しました";
					stat2 |= 0x0001000;
					stat ^= 0x0080000;

				} else if( (stat2 & 0x0001000) == 0 ){	//まだ何も表示されていない場合
					//strdata[7] = null;
					Bugln("Error!\n");
					
					if(rc == 503){
						StrList[15] = new String[3];
						StrList[15][0] = "通信失敗";
						StrList[15][1] = "中間サーバーが";
						StrList[15][2] = "混んでいます。";
						Bugln("Message:503Error\n");
					}else{
						StrList[15] = new String[3];
						StrList[15][0] = "通信失敗(" + errorcode + "," + rc + ")";
						StrList[15][1] = strdata[11].substring(5);
						StrList[15][2] = "";
						i = strdata[11].indexOf(' ');
						if(i > 0){
							StrList[15][2] = strdata[11].substring(i+1);
						}
						Bugln("Message(" + errorcode + "):" + strdata[11] + "\n");
					}
					stat2 |= 0x0001000;
				}
				if((mode & 0x00000002) != 0/*data[78] == 2*/ && (stat & 0x10000) == 0){
					stat2 &= ~0x8000000;	//検索結果表示中フラグ解除
				}
				if((stat & 0x0040000) != 0 && (stat & 0x0000100) == 0/* && (stat2 & 0x0010000) == 0*/){	//レス表示中(+レス続き取得中)また、リストボックスは非表示/*ブックマーク表示中ではない*/
					addCommand(command[0]);//メニュー
					addCommand(command[6]);//戻る
				}
			}
		} catch(Exception e) {
			allprintStackTrace(e);
//				strdata[7] = "致命的なｴﾗｰ";
			//strdata[7] = null;
			StrList[15] = new String[2];
			StrList[15][0] = "致命的なｴﾗｰ";
			StrList[15][1] = e.toString();
			Bugln("致命的なエラー\n");
			Bugln(e.toString() + "\n");
			//bagdata = bagdata + "\r\n" + e.toString();
			stat2 |= 0x0001000;
		}

		//if((stat & 0x0000004) != 0){stat ^= 0x0000004;}	//キーストップ解除
		//data[82] = data[82] / 10;
		///System.out.println("3倍化:" + data[82]);
		stat |= 0x1000000;	//画面更新
		//data[16] = 0;
		stat3 &= ~0x0000810;	//下3つを合体
		//stat3 &= ~0x0000010;	//レスを最後から読むフラグの削除
		//stat3 &= ~0x0000800;	//通信機能使用中
		stat &= ~0x10008014;		//下４つを合体
		//stat &= ~0x0000004;	//キーストップ解除
		//stat &= ~0x0000010;	//通信解除
		//stat &= ~0x0008000;	//スレ一覧のリロード解除
		//stat &= ~0x10000000;	//通信後の処理中解除
		//System.gc();	//ガベージコレクション
	}

	//10+26+26
	//240進数->10進数
	/**
	 * 240進数->10進数
	 */
	public final int to10(byte b[]){
		if(b == null || b.length == 0)
			return 0;
		int i = b.length - 2;
		int j = 240;
		int ret = (b[i+1] & 0xFF) - 16;

		//System.out.println("i:" + (b[b.length - 1] & 0xFF) + "");
		while(i >= 0){
			//System.out.println("i:" + i + " N:" + ((b[i] & 0xFF) - 16) + " J:" + j);
			ret += ((b[i] & 0xFF) - 16) * j;
			i--;	j = j * 240;
		}


		return ret;
	}

	/**
	 * キャッシュクリア処理
	 */
	final void clearcache(){
		int i;
		for(i = 0;i < CacheTitle.length;i++){
			//CacheResData[i] = null;
			//CacheBrdData[i] = null;
			//nCacheSt[i] = 0;
			//nCacheTo[i] = 0;
			nCacheTTL[i] = -1;
		}
		//戻るも初期化
		data[64] = 0;
		strdata[7] = "ｸﾘｱ完了";
		stat2 |= 0x0001000;
	}

	/**
	 * 計算、そして蓄積されるパケットのDLしたサイズや料金などを表示
	 */
	final void viewcost(){
		int i = data[14];

		//strdata[7] = null;
		StrList[15] = new String[8];
		StrList[15][0] = "DLしたｻｲｽﾞ";
		if(data[17] != 0){
			StrList[15][0] = StrList[15][0] + "[g]";
			i = data[17];
		}
		StrList[15][1] = i + "byte";
		StrList[15][2] = "解凍後";
		if(data[18] != 0){
			StrList[15][2] += "(" + ((data[18] - i) * 100 / data[18]) + "%減)";
		}
		StrList[15][3] = data[18] + "/+" + (data[18] - i) + "byte";
		if(data[69] != 0){
			StrList[15][4] = "ﾊﾟｹ代(\\" + (data[69] / 1000) + ".";// + (data[69] % 1000) + "/p)";
			if(data[69] % 1000 < 100){StrList[15][4] += "0";}
			if(data[69] % 1000 < 10){StrList[15][4] += "0";}
			StrList[15][4] += data[69] % 1000 + "/p)";
		} else {

			StrList[15][4] = "ﾊﾟｹ代(\\0.3/p)";
		}
		StrList[15][5] = "約" + data[46] / 100 + ".";
		if(data[46] % 100 < 10){StrList[15][5] += "0";}
		StrList[15][5] += data[46] % 100 + "円";
		StrList[15][6] = "ﾊﾟｹｯﾄ代の累計";
		StrList[15][7] = "約" + data[47] / 100 + ".";
		if(data[47] % 100 < 10){StrList[15][7] += "0";}
		StrList[15][7] += data[47] % 100 + "円";
		stat2 |= 0x0001000;
	}


	/**
	 * 設定をロードする処理
	 */
	public final void Load(){
		//System.out.println("freemem " + (Runtime.getRuntime()).freeMemory());
		DataInputStream isp;
		int i, j;
		RecordStore recordStore;
		byte[] b = null;

		try {
			recordStore = RecordStore.openRecordStore("Setting", false);
			//b = recordStore.getRecord(1);
			isp = new DataInputStream(new ByteArrayInputStream(recordStore.getRecord(1)));
			recordStore.closeRecordStore();//閉じ

			//ByteArrayOutputStream sv = new ByteArrayOutputStream();
			data[0] = isp.readByte() * 127 + isp.readByte();
			data[1] = isp.readByte() * 127 + isp.readByte();
			data[76] = isp.readByte();
			data[82] = isp.readByte() * 127 + isp.readByte();
			data[34] = isp.readByte();
			data[35] = isp.readByte();
			data[36] = isp.readByte();
			data[49] = isp.readByte() & 0xFF;
			data[47] = isp.readInt();
			data[55] = isp.readByte() * 127 + isp.readByte();
			data[56] = isp.readByte();
			i = isp.readByte() & 0xFF;
			if(i > 0){
				b = new byte[i];
				isp.read(b);
				server = new String(b);
			}
			data[57] = isp.readInt();
			i = isp.readByte() & 0xFF;
			if(i > 0){
				b = new byte[i];
				isp.read(b);
				extendedoption = new String(b);
			}
			//i = isp.readByte() & 0xFF;
			data[69] = isp.readInt();
			data[58] = isp.readByte() & 0xFF;	//配色設定
			data[80] = isp.readByte() & 0xFF;	//gzip圧縮率
			data[84] = isp.readInt();	//o 84 先読み機能で先読みを行うレス数
			if(data[84] > 1000){data[84] = 1000;}

			data[87] = isp.readInt();	//o 87 数値の設定
			if((data[87] & 0xFF) == 0){if((data[57] & 0x00000002) != 0){data[87] = 1;} else {data[87] = 2;}}
			if((data[87] & 0xFF00) == 0){if((data[57] & 0x00000004) != 0){data[87] |= 0x0100;} else {data[87] |= 0x0200;}}
			data[90] = isp.readInt();	//o 90 今までに読んだレスの数
			name = isp.readUTF();
			mail = isp.readUTF();
			data[91] = isp.readInt();	//o 91 パケット代警告
			data[92] = isp.readInt();	//o 92 スレッド一覧での表示方法
			data[94] = isp.readInt();	//o 94 7キーの機能
			data[96] = isp.readInt();	//o 96 書き込み画面のURLのﾌﾞﾗｳｻﾞ設定
			data[97] = isp.readInt();	//o 97 電源&電波ﾏｰｸの表示
			data[98] = isp.readInt();	//o 98 文字ｽﾀｲﾙ
			data[99] = isp.readInt();	//o 99 文字ﾌｪｲｽ
			//ｸｯｼｮﾝﾘﾝｸ
			i = isp.readByte() & 0xFF;
			if(i > 0){
				b = new byte[i];
				isp.read(b);
				cushionlink = new String(b);
			}
			data[71] = isp.readInt();	//o 71 7ｷｰの機能 for ﾌﾞｸﾏ
			data[73] = isp.readInt();	//o 71 7ｷｰの機能 for ﾚｽ
			data[74] = isp.readInt();	//o 71 0ｷｰの機能
			//NGワード
			/*i = isp.readByte() & 0xFF;
			if(i > 0){
				b = new byte[i];
				isp.read(b);
				ngword = new String(b);
			}*/
			isp.close();


		} catch(Exception e){
			//System.out.println("Default");

			//デフォルト設定(初回起動時)//////
			data[0] = 10;	//スレの一度に読み込む数
			data[1] = 15;	//レスの一度に読み込む数
			data[76] = 1;	//圧縮率
			data[49] = 0x1D;//0x01 | 0x04 | 0x08 | 0x10
			//stat |= 0x0000200;
			//フォント
			font = Font.getDefaultFont();
			if(font.getHeight() > width / 9/*13*/){data[36] = 0;} else {data[36] = 1;}	//フォントが大きいなら小さくする
			data[34] = 1;	//行間
			data[35] = 5;	//スクロール量
			data[82] = 50;	//スレッドスピード
			data[47] = 0;	//パケット代
			data[80] = 6;	//gzip圧縮率
			data[87] = 0x101;
//			data[57] = 0x00004001;
//			data[57] = 0x0000C001;
			data[57] = 0x00024201;

			//設定ここまで/////////////////////
		}
		if( (data[57] & 0x00000001) == 0 ){
			data[57] = 0x00000001;
		}
		if(server.indexOf("http") != 0){
			//デフォルト設定//////////////////
			server = server_url[0];
/*
			if((System.currentTimeMillis() / 100) % 2 == 0){
				server = "http://imona.net/";
			} else {
				server = "http://imona.zive.net/";
			}
*/
		}

		if(server.substring(server.length() - 1).equals("/") == false){
			server = server + "/";
		}
		//デフォルト設定//////////////////
		if(data[56] == 0){data[56] = 2;}	//ｽﾚ情報の表示		ｽﾚ一覧のﾒﾆｭｰ数
		//ブックマーク
		try {
			int nbookmark = 0;	//ブックマークの数を数えるために使用。

			recordStore = RecordStore.openRecordStore("Bookmark", false);
			b = recordStore.getRecord(1);
			data[40] = b[0] * 127 + b[1];
			data[66] = b[2] * 127 + b[3];

			if(data[40] < 200){
				data[66] = data[40];
				//nbookmark = 0;
				data[40] = 200;
			}
			BookMark = new String[data[40]];
			BookMarkData = new int[data[40]*3];
			data[66] = recordStore.getNumRecords()-1;
			//for(i = 0;i < data[40];i++){
			for(i = 0;i < data[66];i++){
				b = recordStore.getRecord(i + 2);
				StrList[0] = split( new String(b), '\t');


				BookMark[i] = StrList[0][0];
				//tBookMark[i] = trimstr(BookMark[i], width - 29);
				BookMarkData[i*3] = Integer.parseInt(StrList[0][1],36);	//brd
				BookMarkData[i*3+1] = Integer.parseInt(StrList[0][2],36);	//th
				BookMarkData[i*3+2] = Integer.parseInt(StrList[0][3],36);	//res
				if(/*nbookmark >= 0 && */!(BookMark[i].length() == 0)){
					nbookmark = i + 1;
				}
			}

			for(i = data[66];i < data[40];i++){
				BookMark[i] = "";
			}
			//if(nbookmark >= 0){
			data[66] = nbookmark;
			//}

		} catch(Exception e){
			//System.out.println("New BookMark");

			data[40] = 200;	//ブックマークの数
			BookMark = new String[data[40]];
			BookMarkData = new int[data[40]*3];
			for(i = 0;i < data[40];i++){
				BookMark[i] = "";
			}
			//デフォルトのブックマーク
			BookMark[0] = "★携帯用2chﾌﾞﾗｳｻﾞ iMona★";
			BookMark[1] = "[板]iMonaｶﾞｲﾄﾞ";
			BookMark[2] = "[板]2ch総合案内";
			BookMark[3] = "[板]ﾛｸﾞ倉庫";
			BookMarkData[0] = 1108;
			BookMarkData[1] = 1078396443;
			BookMarkData[2] = 9999;
			BookMarkData[3] = 3507;
			//BookMarkData[4] = 0;
			//BookMarkData[5] = 0;
			BookMarkData[6] = 3508;
			//BookMarkData[7] = 0;
			//BookMarkData[8] = 0;
			BookMarkData[9] = 3506;
			//BookMarkData[10] = 0;
			//BookMarkData[11] = 0;
			//tBookMark[0] = trimstr(BookMark[0], width - 29);
			//tBookMark[1] = trimstr(BookMark[1], width - 29);
			//tBookMark[2] = trimstr(BookMark[2], width - 29);
			//tBookMark[3] = trimstr(BookMark[3], width - 29);
			data[66] = 4;
//			e.printStackTrace();
		}
		SetFont();




		//板一覧
		try {
			recordStore = RecordStore.openRecordStore("Brdlist", false);
			brdarray = recordStore.getRecord(1);
			recordStore.closeRecordStore();//閉じ
		} catch(Exception e){
			brdarray = null;
		}
		//brdinit();
		Thread initth = new Thread() {
			public final void run() {
				brdinit();
			}
		};
		initth.start();
		//高速化のためにトリミング済みのブックマーク名をあらかじめ作成
		//for(i = 0;i < BookMark.length;i++){
		//	tBookMark[i] = trimstr(BookMark[i], width - 29);
		//	if(!(tBookMark[i].equals(""))){
		//		data[66] = i + 1;
		//	}
		//}
		Bugln("SettingLoad...Ok!\n");
	}

	/**
	 * ブックマークの保存
	 * @param n
	 */
	public final synchronized void SaveBookMark(int n){
		int /*i = 0, */first = 0;
		byte b[] = new byte[1];
/*
		try {
//			RecordStore.deleteRecordStore("Bookmark");
		} catch(Exception e){
		}
*/
		try {
			String str;

			System.gc();	//ガベージコレクション
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			RecordStore recordStore = RecordStore.openRecordStore("Bookmark", true);
			if(recordStore.getNumRecords() == 0){first = 1;}
			out.write(data[40] / 127);	//o 40 ﾌﾞｯｸﾏｰｸの数
			out.write(data[40] % 127);
			out.write(data[66] / 127);	//o 66 ﾌﾞｯｸﾏｰｸの使用数(間の空白も含む)
			out.write(data[66] % 127);
			//if(first == 1){
			if(recordStore.getNumRecords() == 0){
				recordStore.addRecord(out.toByteArray(), 0, out.toByteArray().length);
			} else {
				recordStore.setRecord( 1, out.toByteArray(), 0, out.toByteArray().length);
			}
			out.close();
			out = null;


			if(first == 1 || n == -1){
				for(n = 0;n < data[40];n++){
					str = BookMark[n] + "\t" + Integer.toString(BookMarkData[n * 3],36) + "\t" + Integer.toString(BookMarkData[n * 3 + 1],36) + "\t" + Integer.toString(BookMarkData[n * 3 + 2],36);
					//if(first == 1){
					if(recordStore.getNextRecordID() <= 2 + n){
						recordStore.addRecord(str.getBytes(), 0, str.getBytes().length);
					} else/* if(BookMarkData[i * 3 + 1] != 0 || !(BookMark[i].equals("")))*/{
						recordStore.setRecord( 2 + n,str.getBytes(), 0, str.getBytes().length);
					}
				}
			} else {
				str = BookMark[n] + "\t" + Integer.toString(BookMarkData[n * 3],36) + "\t" + Integer.toString(BookMarkData[n * 3 + 1],36) + "\t" + Integer.toString(BookMarkData[n * 3 + 2],36);
//				recordStore.setRecord( 2 + n,str.getBytes(), 0, str.getBytes().length);
				if(recordStore.getNextRecordID() <= 2 + n){
					recordStore.addRecord(str.getBytes(), 0, str.getBytes().length);
				} else {
					recordStore.setRecord( 2 + n,str.getBytes(), 0, str.getBytes().length);
				}

			}
			recordStore.closeRecordStore();//閉じ
		} catch(Exception e){
			strdata[7] = "ｴﾗｰ発生" + e.toString();
			stat2 |= 0x0001000;
		}
	}

	/**
	 * 設定の保存
	 */
	public final synchronized void saveSetting(){
		byte b[];// = new byte[1];
		try {
			System.gc();	//ガベージコレクション

			ByteArrayOutputStream out2 = new ByteArrayOutputStream(500);
			DataOutputStream out = new DataOutputStream( out2 );
			//DataOutputStream out = new DataOutputStream( new ByteArrayOutputStream(500) );

			out.write(data[0] / 127);	//o  0 スレの一度に読み込む数
			out.write(data[0] % 127);
			out.write(data[1] / 127);	//o  1 レスの一度に読み込む数
			out.write(data[1] % 127);
			out.write(data[76]);		//o  2 圧縮率
			out.write(data[82] / 127);	//o 17 スレッドのスピード
			out.write(data[82] % 127);
			out.write(data[34]);		//o 34 行間
			out.write(data[35]);		//o 35 １スクロールで移動する量
			out.write(data[36]);		//o 36 文字のサイズ(0:小 1:中 2:大)
			out.write(data[49]);		//o 49 その他設定(オートスクロール・秒表示)

			//WriteInt(out, data[47]);	//o 47 パケット代(合計)
			out.writeInt(data[47]);
			out.write(data[55] / 127);	//o 55 最新ﾚｽで読む数
			out.write(data[55] % 127);
			out.write(data[56]);		//o 56 ｽﾚ情報の表示		ｽﾚ一覧のﾒﾆｭｰ数

			if(server.substring(server.length() - 1).equals("/") == false){
				server = server + "/";
			}
			b = server.getBytes();
			out.write(b.length);	//ｻｰﾊﾞｰ設定
			out.write(b, 0, b.length);
			//WriteInt(out, data[57]);	//o 57 保存するフラグ2
			out.writeInt(data[57]);		//o 57 保存するフラグ2
			if(extendedoption.length() == 0){
				out.write(0x00);
			} else {
				b = extendedoption.getBytes();
				out.write(b.length);	//拡張ｵﾌﾟｼｮﾝ
				out.write(b, 0, b.length);
			}
			out.writeInt(data[69]);		//o 69 パケット代の単価(\/1000packet \0.3/packet->\300/1000packet) 0の場合は自動
			out.write(data[58]);		//o 58 配色設定
			out.write(data[80]);		//o 80 gzipの圧縮率
			out.writeInt(data[84]);		//o 84 先読み機能で先読みを行うレス数
			out.writeInt(data[87]);		//o 87 数値の設定
			out.writeInt(data[90]);		//o 90 今までに読んだレスの数
			if(name.getBytes().length == 0){
				out.writeShort(0);
			} else {
				//b = name.getBytes();
				//out.writeShort(b.length);		//書き込み時の名前
				//out.write(b, 0, b.length);
				out.writeUTF(name);
			}
			if(mail.getBytes().length == 0){
				out.writeShort(0);
			} else {
				//b = mail.getBytes();
				//out.writeShort(b.length);		//書き込み時のメアド
				//out.write(b, 0, b.length);
				out.writeUTF(mail);
			}
			//out.writeUTF(name);
			//out.writeUTF(mail);
			out.writeInt(data[91]);		//o 91 パケット代警告
			out.writeInt(data[92]);		//o 92 スレッド一覧での表示方法
			out.writeInt(data[94]);		//o 94 7キーの機能
			out.writeInt(data[96]);		//o 95 書込画面のURL
			out.writeInt(data[97]);		//o 97 電池&電波マークの表示時間
			out.writeInt(data[98]);		//o 98 文字ｽﾀｲﾙ
			out.writeInt(data[99]);		//o 99 文字ﾌｪｲｽ
			//ｸｯｼｮﾝﾘﾝｸ
			if(cushionlink.length() == 0){
				out.write(0x00);
			} else {
				b = cushionlink.getBytes();
				out.write(b.length);	//ｸｯｼｮﾝﾘﾝｸ
				out.write(b, 0, b.length);
			}
			out.writeInt(data[71]);		//o 71 7ｷｰの機能　for ﾌﾞｸﾏ
			out.writeInt(data[73]);		//o 73 7ｷｰの機能　for ｽﾚ
			out.writeInt(data[74]);		//o 74 0ｷｰの機能
			/*if(ngword.equals("")){
				out.write(0x00);
			} else {
				b = ngword.getBytes();
				out.write(b.length);	//ｸｯｼｮﾝﾘﾝｸ
				out.write(b, 0, b.length);
			}*/
			out.writeInt(0);
			out.writeInt(0);
			out.writeInt(0);

			RecordStore recordStore = RecordStore.openRecordStore("Setting", true);

			//書き込み
			b = out2.toByteArray();
			if(recordStore.getNumRecords() == 0){
				recordStore.addRecord(b, 0, b.length);
				//recordStore.addRecord(out2.toByteArray(), 0, out2.toByteArray().length);
			} else {
				recordStore.setRecord( 1, b, 0, b.length);
				//recordStore.setRecord( 1, out2.toByteArray(), 0, out2.toByteArray().length);
			}
			recordStore.closeRecordStore();//閉じ
			out2.reset();//初期化

			//SaveBookMark(-1);
		} catch(Exception e){
			strdata[7] = e.toString();
			stat2 |= 0x0001000;
		}
	}

	/**
	 * 文字列を分割
	 * @param str 対象文字列
	 * @param ch
	 * @return String
	 */
	public final String[] split( final String str, int ch){
		Vector vecStr = new Vector();
		int end, start = 0;
		while(true){
			end = str.indexOf(ch , start);
			if(end == -1){
				vecStr.addElement(str.substring(start));
				break;
			} else {// 決められた文字があったとき
				vecStr.addElement(str.substring(start, end));
				start = end + 1;
			}
		}
		String[] result = new String[vecStr.size()];
		vecStr.copyInto(result);
		return result;
	}

	/**
	 * メインメニュー
	 * @param i 選択した箇所
	 */
	public final void mainmenu(int i){
		switch(i){
			case 0:	//板一覧
				//カテゴリリスト読み込み済みフラグがたつまで待つ
				while((stat & 0x0000800) == 0){
					try{Thread.sleep(10);}catch (Exception e){}
				}

				ListBox = StrList[12];
				stat |= 0x2000;	//カテゴリ選択中
			break;
			case 1:	//ﾌﾞｯｸﾏｰｸ
				data[67] = 0; data[68] = 0;
				showBookMark(0);
			break;
			case 2:	//URL指定
				stat2 &= ~0x0004000;	//function解除

				tfield = new LocalizedTextField("ｽﾚ･板のURL","",300,LocalizedTextField.ANY);
				inputForm = new Form("URL指定");
				inputForm.append(tfield);

				inputForm.addCommand(command[8]);
				inputForm.addCommand(command[2]);

				inputForm.setCommandListener(this);

				disp.setCurrent(inputForm);

				stat3 |= 0x0001000;
			break;
			case 3:	//設定
				ListBox = StrList[4];
				stat2 |= 0x0000001;	//設定
			break;
			//case 3:	//データフォルダ
				//-/DataFolder = GetFileList(0);
				//-/ListBox = GetNameList(DataFolder);
			//-/stat |= 0x0000200;	//データフォルダー
			//break;
			case 4:	//終了
				stat |= 0x0004;	//キーロック
				//SaveSetting();
				parent.destroyApp(false);
				parent.notifyDestroyed();
			break;
		}
	}

	/**
	 * スレッドメニュー
	 * @param i 選択した箇所
	 */
	public final void threadmenu(int i){
		switch(i){
			case 0:	//一覧に戻る もどる
				backfromfirst();
			break;
			case 1:	//最新ﾚｽをみる
				httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,-1,data[55],nCacheTh[nCacheIndex]/*nThread*/[data[60]]);
				stat |= 0x40000000;	//Loading中の操作は不可能
				break;
				case 2:	//ﾚｽ番号を指定
				if((stat & 0x0000400) != 0){	//設定した後
					//setResnum(nCacheBrd[nCacheIndex]/*data[3]*/, nCacheTh[nCacheIndex]/*nThread*/[data[4]]/*data[2]*/);
					setResnum(nCacheBrd[nCacheIndex]/*data[3]*/, nCacheTh[nCacheIndex]/*nThread*/[data[60]]/*data[2]*/);
				} else {
					setResnum(nCacheBrd[nCacheIndex], 1);
				}
			break;
			case 3:	//1のみ取得
				httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,1,1,nCacheTh[nCacheIndex]/*nThread*/[data[60]]);
			break;
			case 4:	//ﾌﾞｯｸﾏｰｸに登録
				//if(-1 == EditBookMark( 0, CacheBrdData[nCacheIndex]/*ThreadName*/[data[4]], nCacheBrd[nCacheIndex]/*data[3]*/, nCacheTh[nCacheIndex]/*nThread*//*nThread*/[data[4]], 0)){
				if(-1 == EditBookMark( 0, CacheBrdData[nCacheIndex]/*ThreadName*/[data[60]], nCacheBrd[nCacheIndex]/*data[3]*/, nCacheTh[nCacheIndex]/*nThread*//*nThread*/[data[60]], 0)){
					//strdata[7] = null;
					//StrList[15] = new String[3];
					//StrList[15][0] = "= 登録失敗 =";
					//StrList[15][1] = "ﾌﾞｯｸﾏｰｸがいっ";
					//StrList[15][2] = "ぱいです。";
					strdata[7] = "登録失敗";
					stat2 |= 0x0001000;
				}
			break;
			case 5:	//ｽﾚｯﾄﾞを立てる
				//if(strdata[1] != null && data[79] == nCacheBrd[nCacheIndex]/*data[3]*/){
				openbrowser(server + "2.cgi?v=C&m=w2&b=" + nCacheBrd[nCacheIndex]/*data[3]*/ , 1);
				/*} else {
					httpinit(6, nCacheBrd[nCacheIndex], 0, 0, 0);//dlbrdlist();
				}*/
				//break;
			case 6:	//再読込
				if((stat2 & 0x8000000) == 0) {
				//	stat ^= 0x10000;	//ｽﾚ選択中解除
					//stat |= 0x8000;	//スレ一覧ＤＬ中
					strdata[8] = strdata[9];	//現在のタイトルを使用する

					stat |= 0x0008000;	//スレ一覧のリロード
	//				i = data[5] - data[4] + 1;
					//data[5] = i - 1;
	//				httpinit(1,nCacheBrd[nCacheIndex]/*data[3]*/,i,i + data[0] - 1,0);
					httpinit(1,nCacheBrd[nCacheIndex]/*data[3]*/,nCacheSt[nCacheIndex],nCacheSt[nCacheIndex] + data[0] - 1,0);
				}
			break;
			case 7:	//ｷｬｯｼｭｸﾘｱ
				clearcache();
			break;
			case 8:	//通信の詳細
				viewcost();
			break;

			case 9:	//ﾒﾆｭｰを閉じる
				//backfromthreadlist();
				stat &= ~0x0000100;	//ﾒﾆｭｰ解除
				addCommand(command[0]);
				addCommand(command[6]);
			break;
		}
	}

	/**
	 * レスメニュー
	 * @param i 選択した箇所
	 * @param j 多分、現在のレス数
	 */
	public final void resmenu(int i, int j){
		switch(i){
			case 0:	//ｽﾚ一覧に戻る
				backfromfirst();
			break;
			case 1:	//最新ﾚｽ
				httpinit(2,nCacheBrd[nCacheIndex]/*data[3]*/,-1,data[55],nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/);
				stat |= 0x40000000;	//Loading中の操作は不可能
				//stat &= ~0x0040000;	//メニューを表示させるために一時的にレス表示中を解除
				//viewres();
				addCommand(command[0]);//メニュー
				addCommand(command[6]);//戻る
				stat &= ~0x0000100;	//リストボックス解除
			break;

			case 2:	//ﾚｽ番号　指定位置に移動　ﾚｽ番号を指定　ﾚｽ番指定
				if((stat & 0x0000400) != 0){	//設定した後
					setResnum(nCacheBrd[nCacheIndex]/*data[3]*/, nCacheTh[nCacheIndex][0]/*data[2]*/);
					//stat &= ~0x0040000;	//メニューを表示させるために一時的にレス表示中を解除
					//viewres();
					addCommand(command[0]);//メニュー
					addCommand(command[6]);//戻る
					stat &= ~0x0000100;	//リストボックス解除
				} else {
					setResnum(nCacheBrd[nCacheIndex], data[6] + nCacheSt[nCacheIndex]/*data[7]*/);
				}
			break;

			case 3:	//書き込み　書込&終了
				if((data[57] & 0x00080000) != 0 & bodytext.indexOf(">>") <= 0 & bodytext.length() <= 5){bodytext = ">>" + Integer.toString(data[6] + nCacheSt[nCacheIndex]) + "\n";}
				if(strdata[1] != null && data[79] == nCacheBrd[nCacheIndex]/*data[3]*/){
					viewwritepanel();
				} else {
					stat4 |= 0x0040000;
					httpinit(6, nCacheBrd[nCacheIndex]/*data[3]*/, 0, 0, 0);//dlbrdlist();
				}
			break;
			//case 4:	//ここにレス
			//	bodytext = bodytext + ">>" + Integer.toString(data[6] + nCacheSt[nCacheIndex]) + "\n";
			//	if(strdata[1] != null && data[79] == nCacheBrd[nCacheIndex]/*data[3]*/){
			//		viewwritepanel();
			//	} else {
			//		stat4 |= 0x0040000;
			//		httpinit(6, nCacheBrd[nCacheIndex]/*data[3]*/, 0, 0, 0);//dlbrdlist();
			//	}
			//break;
			case 4:	//書込画面のURL
				stat2 &= ~0x0004000;	//function解除

				try{
				String buf = server + "2.cgi?v=C&m=w&b=" + nCacheBrd[nCacheIndex]/*data[3]*/ + "&t=" + nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/;
				if(data[96] == 1){
					//System.out.println(buf);
					openbrowser(buf, 1);
				} else {
					tbox = new LocalizedTextBox(StrList[3][i], buf, buf.length(), LocalizedTextField.ANY);
					tbox.addCommand(command[2]);
					stat3 |= 0x0004000;
					tbox.setCommandListener(this);
					disp.setCurrent(tbox);
				}
				} catch(Exception e){
					stat3 &= ~0x0004000;
					disp.setCurrent(this);
				}
			break;
			case 5:	//ﾌﾞｯｸﾏｰｸ
				showBookMark(0);
			break;
			case 6:	//ﾌﾞｯｸﾏｰｸに登録
				if(-1 == EditBookMark( 0, CacheTitle[nCacheIndex]/*strdata[9]*//*ThreadName[data[4]]*/, nCacheBrd[nCacheIndex]/*data[3]*/, nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/, data[6]+nCacheSt[nCacheIndex]/*data[7]*/)){
					//strdata[7] = null;
					//StrList[15] = new String[3];
					//StrList[15][0] = "= 登録失敗 =";
					//StrList[15][1] = "ﾌﾞｯｸﾏｰｸがいっ";
					//StrList[15][2] = "ぱいです。";
					strdata[7] = "登録失敗";
					stat2 |= 0x0001000;
				}
			break;

			case 7:	//設定
				ListBox = StrList[4];
				stat2 |= 0x0000001;	//設定
				//data[19] = ListBox.length;
				data[10] = 0;data[11] = 0;
			break;
			case 8:	//ﾃｷｽﾄﾎﾞｯｸｽ
				stat2 &= ~0x0004000;	//function解除

				try{
					/*String buf = "";
					for(i = 0; i < data[85]; i++){
						buf = buf + CacheResData[nCacheIndex][i][0]  + "\n";
					}
					tbox = new LocalizedTextBox("ﾚｽ", buf, buf.length(), LocalizedTextField.ANY);*/
					//System.out.println("\r\n\r\n-------" + resstr + "------\r\n\r\n");
					String r_resdata = new String(resdata);
					r_resdata = r_resdata.replace('\t','\r');
					r_resdata = Integer.toString(data[6] + nCacheSt[nCacheIndex]) + ":" + strdata[3] + "\r" + r_resdata;
					tbox = new LocalizedTextBox("ﾚｽ", r_resdata, r_resdata.length(), LocalizedTextField.ANY);
					//inputForm = new Form(StrList[3][i]);
					//inputForm.addCommand(command[4]);
					tbox.addCommand(command[2]);
					stat3 |= 0x0002000;
					//inputForm.append(tfield);
					//inputForm.append(new TextBox("ﾚｽ", buf, buf.length(), TextField.ANY));
					tbox.setCommandListener(this);
					disp.setCurrent(tbox);
				} catch(Exception e){
					stat3 &= ~0x0002000;
					System.gc();
					disp.setCurrent(this);
				}
				stat |= 0x1000000;	//画面更新
			break;
			case 9:	//ﾃｷｽﾄﾎﾞｯｸｽ(URL)
				stat2 &= ~0x0004000;	//function解除
				try{
					if(data[60] != -1){
						String buf = "";

						i = Linklist2[data[60]] % 1000;
						j = Linklist[data[60]] / 1000;
						if(j % 10000 != 0){i += iDivStr[j % 10000-1];}
						buf = resstr.substring(i, i + (j / 10000));
						tbox = new LocalizedTextBox("URL", buf, buf.length(), LocalizedTextField.ANY);
						tbox.addCommand(command[2]);
						stat3 |= 0x0002000;
						tbox.setCommandListener(this);
						disp.setCurrent(tbox);
					}
				} catch(Exception e){
					stat3 &= ~0x0002000;
					System.gc();
					disp.setCurrent(this);
				}
			break;

			case 10:	//ｷｬｯｼｭｸﾘｱ
				clearcache();
			break;
			case 11:	//AAS
				if(strdata[1] != null && data[79] == nCacheBrd[nCacheIndex]/*data[3]*/){
					//data[78] |= 0x0000400; //AAS
					strdata[0] = strdata[1];
					if(strdata[0].charAt(strdata[0].length() - 1) == '/'){
						strdata[0] = strdata[0].substring( 0, strdata[0].length() - 1);
					}
					int it = strdata[0].lastIndexOf( '/', strdata[0].length() - 1);
					bbsname = strdata[0].substring(it + 1);
					strdata[0] = strdata[0].substring( 0, it);
					String buf = "example.ddo.jp/aas/a.i/" + strdata[0] + "/" + bbsname + "/" + nCacheTh[nCacheIndex][0] + "/" + (data[6] + nCacheSt[nCacheIndex]);

					openbrowser(buf,1);
				}else{
					httpinit(6, nCacheBrd[nCacheIndex]/*data[3]*/, 0, 0, 0);//dlbrdlist();
				}

			break;
			/*case 12:   //ｽﾚｯﾄﾞの詳細
				StrList[15] = new String[2];
				StrList[15][0] = "ｽﾚｯﾄﾞの詳細";
				//StrList[15][1] = e.toString();
				stat2 |= 0x0001000;
			break;*/
			/*case 12://c.2chでのｱｸｾｽ
				if(strdata[1] == null && data[79] != nCacheBrd[nCacheIndex]/*data[3]){
					data[78] |= 0x0000800; //c.2ch.net
					httpinit(6, nCacheBrd[nCacheIndex]/*data[3], 0, 0, 0);//dlbrdlist();
				}else{
					strdata[0] = strdata[1];
					if(strdata[0].charAt(strdata[0].length() - 1) == '/'){
						strdata[0] = strdata[0].substring( 0, strdata[0].length() - 1);
					}
					int it = strdata[0].lastIndexOf( '/', strdata[0].length() - 1);
					bbsname = strdata[0].substring(it + 1);
					//strdata[0] = strdata[0].substring( 0, it);
					String buf = "c.2ch.net/test/-/" + bbsname + "/" + nCacheTh[nCacheIndex][0] + "/" + (data[6] + nCacheSt[nCacheIndex]) + "-";

					openbrowser(buf,1);
				}
			break;*/
			case 12:	//通信の詳細
				viewcost();
			break;
			case 13:	//ﾒﾆｭｰを閉じる
				stat ^= 0x0100;
				addCommand(command[0]);
				addCommand(command[6]);
			break;
		}
	}

	/**
	 * ブックマークメニュー(レス)
	 * @param i 選択した箇所
	 * @param j　ブックマークの選択している箇所
	 */
	public final void bookmarkmenu(int i, int j){ //ﾌﾞｯｸﾏｰｸﾒﾆｭｰ(ﾚｽ)
		int k = 0;
		if(i == 0){//最新ﾚｽを読む
			httpinit(2,BookMarkData[j * 3],-1,data[55],BookMarkData[j * 3 + 1]);
			thttpget();
		} else if(i == 1){//ﾚｽ番指定
			if(0 < BookMarkData[j * 3 + 2] && BookMarkData[j * 3 + 2] < 9999 ){
				setResnum(BookMarkData[j * 3], BookMarkData[j * 3 + 2]);
			} else {
				setResnum(BookMarkData[j * 3], 1);
			}
			/*if((stat4 & 0x0000800) != 0){
				addCommand(command[2]);
			}*/
		} else if(i == 2){//書込&終了
//			openbrowser(server.substring(7) + "2.cgi?v=C&m=w&b=" + BookMarkData[j * 3] + "&t=" + BookMarkData[j * 3 + 1]);
			if(strdata[1] != null && data[79] == BookMarkData[j * 3]){
				viewwritepanel();
			} else {
				stat4 |= 0x0040000;
				httpinit(6, BookMarkData[j * 3], 0, 0, 0);//dlbrdlist();
			}
		} else if(i == 3){//書込画面のURL
			stat2 &= ~0x0004000;	//function解除

			try{
				String buf = server + "2.cgi?v=C&m=w&b=" + BookMarkData[j * 3] + "&t=" + BookMarkData[j * 3 + 1];
				if(data[96] == 1){
					//System.out.println(buf);
					openbrowser(buf, 1);
					//BrowserConnection conn = (BrowserConnection)Connector.open( "url://" + buf);
					//conn.connect();
					//conn.close();
					//stat |= 0x1000000;	//画面更新
				}else{
					tbox = new LocalizedTextBox("書込画面のURL", buf, buf.length(), LocalizedTextField.ANY);
					tbox.addCommand(command[2]);
					stat3 |= 0x0004000;
					tbox.setCommandListener(this);
					disp.setCurrent(tbox);
				}
			} catch(Exception e){
				stat3 &= ~0x0004000;
				disp.setCurrent(this);
			}
		} else if(i == 4){//検索
			commandAction(command[7]);
		} else if(i == 5){	//編集
			commandAction(command[3]);
		} else if(i == 6){//ﾌﾞｯｸﾏｰｸの消去
			EditBookMark( - j - 1, "", 0, 0, 0);	//消去
		} else if(i == 7){//隙間を詰める
			int n = data[66];
			for(j = 0; j < data[66]; j++){
				if(BookMarkData[j * 3 + 1] == 0 && BookMark[j].length() == 0){
					for(k = j+1; k < data[66]; k++){
						if(!(BookMarkData[k * 3 + 1] == 0 && BookMark[k].equals(""))){
							ChangeBookMark(j, k);	n = j + 1;
							break;
						}
					}
				}
			}
			data[66] = n;
		} else if(i == 8 && data[66] < data[40]){//隙間を作る
			for(k = data[66]; k > j; k--){
				ChangeBookMark(k-1, k);
			}
			data[66]++;
		} else if(i == 9){//ｴｸｽﾎﾟｰﾄ		ｴｸｽ/ｲﾝﾎﾟｰﾄ
			try{
				String buf = "iB", buf2;
				if((stat2 & 0x0004000) != 0){	//function
					if(BookMarkData[j * 3 + 1] == 0 && BookMark[j].length() == 0){j++;}
					k = j + 10;
					if(k > data[66]){k = data[66];}
				} else {
					j = 0;	k = data[66];
				}

				for(; j < k; j++){
					buf2 = BookMark[j];
					while(true){
						i = buf2.indexOf(",");	if(i < 0){break;}
						buf2 = buf2.substring(0,i) + "%2C" + buf2.substring(i+2);
					}
					buf += "," + buf2 + "," + Integer.toString(BookMarkData[j * 3],36) + "," + Integer.toString(BookMarkData[j * 3 + 1],36) + "," + Integer.toString(BookMarkData[j * 3 + 2],36);
				}

				tbox = new LocalizedTextBox("ｴｸｽﾎﾟｰﾄ", buf, buf.length(), LocalizedTextField.ANY);
				tbox.addCommand(command[2]);
				stat3 |= 0x2000000;
				tbox.setCommandListener(this);
				disp.setCurrent(tbox);
			} catch(Exception e){
				stat3 &= ~0x2000000;
				disp.setCurrent(this);
			}
		} else if(i == 10){//ｲﾝﾎﾟｰﾄ
			try{
				tbox = new LocalizedTextBox("ｲﾝﾎﾟｰﾄ", null, 5000, LocalizedTextField.ANY);
				tbox.addCommand(command[8]);
				tbox.addCommand(command[2]);
				stat3 |= 0x4000000;
				tbox.setCommandListener(this);
				disp.setCurrent(tbox);
			} catch(Exception e){
				stat3 &= ~0x4000000;
				disp.setCurrent(this);
			}
		}/* else if(i == 11){//全消去
			try{
				tbox = new LocalizedTextBox("ｲﾝﾎﾟｰﾄ", null, 5000, LocalizedTextField.ANY);
				tbox.addCommand(command[8]);
				tbox.addCommand(command[2]);
				stat3 |= 0x4000000;
				tbox.setCommandListener(this);
				disp.setCurrent(tbox);
			} catch(Exception e){
				stat3 &= ~0x4000000;
				disp.setCurrent(this);
			}
		}*/
	}

	/**
	 * 設定トップメニュー
	 * @param i 選択した箇所
	 */
	public final void settingmenu(int i){
		switch(i){
			case 0:	//板ﾘｽﾄの更新,板ﾘｽﾄ更新
				//Load();

				httpinit(0,0,0,0,0);//dlbrdlist();

			break;
			case 1:	//設定の保存
				saveSetting();
				strdata[7] = "保存完了";//StrList[10][11] + StrList[10][19];
				stat2 |= 0x0001000;
			break;
			case 2:	//表示設定
				ListBox = StrList[5];
				//stat2 ^= 0x0000001;	//設定
				stat2 |= 0x0000002;	//表示設定
			break;
			case 3:	//操作設定
				ListBox = StrList[6];
				//stat2 ^= 0x0000001;	//設定
				stat2 |= 0x0000004;	//操作設定
			break;
/*
			case 3:	//色の設定
			//	ListBox = ColSetMenu;
				//stat2 ^= 0x0000001;	//設定
			//	stat2 |= 0x0000004;	//色の設定
			break;
*/
			case 4:	//通信設定
				ListBox = StrList[7];
				//stat2 ^= 0x0000001;	//設定
				stat2 |= 0x0000008;	//通信設定
			break;

			case 5:	//その他
				ListBox = StrList[8];
				stat2 |= 0x0000020;	//その他
			break;
			case 6:	//ﾒｲﾝﾒﾆｭｰに戻る
				if((stat & 0x40000) != 0){	//レス表示中
					ListBox = StrList[3];
				} else {
					ListBox = StrList[1];
				}
				stat2 ^= 0x0000001;	//設定解除
			break;
		}
	}

	/**
	 * 表示設定メニュー
	 * @param i 選択した箇所
	 */
	public final void viewmenu(int i){

		switch(i){
		case 0:	//文字ｻｲｽﾞ
			if((stat & 0x0000400) != 0){	//設定した後
				data[36] = data[28];
				stat ^= 0x0000400;
				stat2 ^= 0x0000200;	//１文字
				SetFont();
				if((stat & 0x40000) != 0){	//レス表示中
					makeRes(/*0*/);
				}
			} else {
				stat |= 0x0000400;
				stat2 |= 0x0000200;	//１文字
				data[26] = 2;	//数
				data[27] = 0;	//桁
				data[28] = data[36];	//初期値
				strdata[2] = StrList[5][i];
				StrList[14] = StrList[10];
			}
			break;
		case 1:	//文字ｽﾀｲﾙ
			if((stat & 0x0000400) != 0){	//設定した後
				data[98] = data[28];
				stat ^= 0x0000400;
				stat2 ^= 0x0000200;	//１文字
				SetFont();
				if((stat & 0x40000) != 0){	//レス表示中
					makeRes(/*0*/);
				}
			} else {
				stat |= 0x0000400;
				stat2 |= 0x0000200;	//１文字
				data[26] = 2;	//数
				data[27] = 0;	//桁
				data[28] = data[98];	//初期値
				strdata[2] = StrList[5][i];
				StrList[14] = StrList[35];
			}
			//System.out.println(data[98]);
			break;
		case 2:	//文字ﾌｪｲｽ
			if((stat & 0x0000400) != 0){	//設定した後
				stat ^= 0x0000400;
				stat2 ^= 0x0000100;	//数字
				data[99] = data[28];
				stat2 ^= 0x1000000;
			} else{
				NumSet();
				stat |= 0x0000400;
				stat2 |= 0x0000100;	//数字
				data[26] = 2;	//上限
				data[27] = 0;	//桁
				data[28] = data[99];	//初期値
				StrList[15] = StrList[36];
				stat2 ^= 0x0002000;
				strdata[2] = StrList[5][i];

				stat2 |= 0x1000000;
			}
			break;
		case 3:	//行間
			if((stat & 0x0000400) != 0){	//設定した後
				stat ^= 0x0000400;
				stat2 ^= 0x0000100;	//数字
				data[34] = data[28];
			} else {
				stat |= 0x0000400;
				stat2 |= 0x0000100;	//数字
				data[26] = 99;	//上限
				data[27] = 0;	//桁
				data[28] = data[34];	//初期値
				data[29] = 1;	//桁数－１
				strdata[2] = StrList[5][i];
			}
			break;
//			case 4:	//ｽﾚ一覧のﾒﾆｭｰ数
		case 7:	//背面液晶
			if((stat & 0x0000400) != 0){	//設定した後
				stat ^= 0x0000400;
				stat2 ^= 0x0000100;	//数字
//				if(i == 4){
//				data[56] = data[28];
//				} else {
				if(data[28] == 0){
					data[57] &= ~0x00000070;
				} else {
					data[57] |= 0x00000010;

					if(data[28] >= 3){
						data[57] |= 0x00000040;
						data[28] -= 2;
					} else {
						data[57] &= ~0x00000040;
					}
					if(data[28] == 1){
						data[57] &= ~0x00000020;
					} else {
						data[57] |= 0x00000020;
					}
				}
//				data[51] = height / 2;
//				data[53] = height / 2;
				stat2 ^= 0x1000000;
//				}
				stat2 &= ~0x0002000;
			} else {
				NumSet();
//				if(i == 4){
//				data[28] = data[56];	//初期値
//
//				data[26] = 9;	//上限
//				} else {	//1:mona 2:nomal 3:mona+star 4:nomal+star
				stat2 ^= 0x0002000;
				if((data[57] & 0x00000010) == 0){
					data[28] = 0;
				} else {
					if((data[57] & 0x00000020) != 0){data[28] = 2;
					} else {data[28] = 1;}
					if((data[57] & 0x00000040) != 0){data[28] += 2;}
				}
				data[26] = 4;	//上限
				//strdata[7] = null;
				/*
				 StrList[15] = new String[3];
				 StrList[15][0] = "0:無し";
				 StrList[15][1] = "1:Mona 2:Nomal";
				 StrList[15][2] = "3:1+星 4:2+星";
				 */
				StrList[15] = StrList[30];
//				data[51] -= data[30] * 2;
//				data[53] += data[30] + 5;
				stat2 |= 0x1000000;
//				}
				data[29] = 0;	//桁数－１
				strdata[2] = StrList[5][i];
			}
			break;
		case 4: //日時表示
		case 5:	//時刻表示
		case 6:	//秒表示
		case 8:	//ﾊｲｺﾝﾄﾗｽﾄ
		case 9:	//壁紙非表示
		case 10:	//ｷｬｯｼｭ情報の表示
		case 11:	//ｽﾚ情報の表示
			if((stat & 0x0000400) != 0){	//設定した後
				stat ^= 0x0000400;
				stat2 ^= 0x0000200;	//１文字
				if(data[28] == 1){
					if(i == 4){
						data[57] |= 0x00040000;
					}else if(i == 5){	//時刻表示
						data[57] |= 0x00000008;
					} else if(i == 6) {	//秒表示
						data[49] |= 0x02;
					} else if(i == 8){
						//data[57] |= 0x00000100;
						data[58] = 1;
						ColScm = ColPreset[1];
					} else if(i == 9) {
						data[57] |= 0x00000400;
					} else if(i == 10){
						data[57] |= 0x00004000;
						chkcache();
					} else if(i == 11){
						data[56] = 2;
					}
				} else {
					if(i == 4){
						data[57] &= ~0x00040000;
					}else if(i == 5){	//時刻表示
						data[57] &= ~0x00000008;
					} else if(i == 6) {	//秒表示
						data[49] &= ~0x02;
					} else if(i == 8){
						//data[57] &= ~0x00000100;
						if(data[58] == 1){
							data[58] = 0;
							ColScm = ColPreset[0];
						}
					} else if(i == 9){
						data[57] &= ~0x00000400;
					} else if(i == 10){
						data[57] &= ~0x00004000;
					} else if(i == 11){
						data[56] = 1;
					}
				}
			} else {
				stat |= 0x0000400;
				stat2 |= 0x0000200;	//１文字
				data[26] = 1;	//上限
				data[27] = 0;	//桁
				data[28] = 0;	//初期値

				if(i == 4){
					if((data[57] & 0x00040000) != 0)
						data[28] = 1;	//初期値
				}else if(i == 5){	//時刻表示
					if((data[57] & 0x00000008) != 0)
						data[28] = 1;	//初期値
				} else if(i == 6){	//秒表示
					if((data[49] & 0x02) != 0)
						data[28] = 1;	//初期値
				} else if(i == 8){
					if(data[58] == 1){
						data[28] = 1;	//初期値
					}
					//if((data[57] & 0x00000100) != 0)
					//	data[28] = 1;	//初期値
				} else if(i == 9){
					if((data[57] & 0x00000400) != 0){data[28] = 1;}	//初期値
				} else if(i == 10){
					if((data[57] & 0x00004000) != 0){data[28] = 1;}
					/*
					 StrList[15] = new String[3];
					 StrList[15][0] = "ｷｬｯｼｭの存在を";
					 StrList[15][1] = "ﾁｪｯｸしてﾘﾝｸの";
					 StrList[15][2] = "色を変えます｡";
					 data[51] -= data[30] * 2;
					 data[53] += data[30] + 5;
					 stat2 |= 0x1000000;
					 */
				} else if(i == 11){
					if(data[56] > 1){data[28] = 1;}
				}
				data[29] = 1;	//桁数－１
				strdata[2] = StrList[5][i];
				StrList[14] = StrList[11];
			}
			break;
		case 12:	//電池と電波のﾏｰｸ
			if((stat & 0x0000400) != 0){	//設定した後
				stat ^= 0x0000400;
				stat2 ^= 0x0000100;	//数字
				data[97] = data[28];
				stat2 ^= 0x1000000;
				stat2 &= ~0x0002000;
			} else {
				NumSet();

				stat2 ^= 0x0002000;
				data[28] = data[97];
				data[26] = 99999;	//上限
				data[27] = 4;	//桁
				data[28] = data[97];	//初期値
				data[29] = 4;	//桁数－１
				//data[26] = 9999;	//上限
				StrList[15] = StrList[34];
				stat2 |= 0x1000000;
				//data[29] = 4;	//桁数－１
				strdata[2] = StrList[5][i];
			}
			break;

		case 13:	//戻る
			stat2 ^= 0x0000002;
			ListBox = StrList[4];
			//data[19] = ListBox.length;
			//data[10] = data[11] = 0;
			data[10] = data[20];	data[11] = data[21];
			break;
		}
	}

	/**　
	 * 操作メニュー
	 * @param i 選択した箇所
	 */
	public final void contmenu(int i){
		switch(i){
			case 0:	//ｽｸﾛｰﾙ量
			case 1:	//ｽｸﾛｰﾙ方法(ｽﾚ覧)
				if((stat & 0x0000400) != 0){	//設定した後
					stat ^= 0x0000400;
					stat2 ^= 0x0000100;	//数字
					if(i == 0){
						data[35] = data[28];
					} else {
						data[57] &= ~0x00028000;
						if(data[28] == 1){
							data[57] |= 0x00008000;
						} else if(data[28] == 2){
							data[57] |= 0x00020000;
						}
	//					data[51] = height / 2;
	//					data[53] = height / 2;
						stat2 ^= 0x1000000;
					}
				} else {
					stat |= 0x0000400;
					stat2 |= 0x0000100;	//数字
					if(i == 0){
						data[26] = 99;	//上限
						data[27] = 0;	//桁
						data[28] = data[35];	//初期値
						data[29] = 1;	//桁数－１
					} else {
						data[26] = 2;	//上限
						data[27] = 0;	//桁
						data[28] = 0;	//初期値
						if((data[57] & 0x00008000) != 0){
							data[28] = 1;	//初期値
						} else if((data[57] & 0x00020000) != 0){
							data[28] = 2;	//初期値
						}
						data[29] = 0;	//桁数－１

						StrList[15] = StrList[32];
	//					data[51] -= data[30] * 2;	data[53] += data[30] + 5;
						stat2 |= 0x1000000;
					}
					strdata[2] = StrList[6][i];
				}
				break;

			case 2:	//ｵｰﾄｽｸﾛｰﾙ
			case 3:	//SO用ｽｸﾛｰﾙ処理
			case 4://AAモード時に6.4キーで移動
				if((stat & 0x0000400) != 0){	//設定した後
					stat ^= 0x0000400;
					stat2 ^= 0x0000200;	//１文字
					if(data[28] == 1){
						if(i == 2){
							data[49] |= 0x01;
						} else if(i == 3){
							data[49] |= 0x20;
						} else if(i == 4) {
							data[49] |= 0x100;
						}
						//data[49] &= ~0x01;	//ｵｰﾄｽｸﾛｰﾙ
					} else {
						if(i == 2){
							data[49] &= ~0x01;
						} else if(i == 3){
							data[49] &= ~0x20;
						} else if(i == 4) {
							data[49] &= ~0x100;
						}
					}
					//容量を稼ぐために消去
					/*
					 data[51] = height / 2;
					 data[53] = height / 2;
					 */
	//				stat2 ^= 0x1000000;
				} else {
					stat |= 0x0000400;
					stat2 |= 0x0000200;	//１文字
					data[26] = 1;	//上限
					data[27] = 0;	//桁
					data[28] = 0;	//初期値
					if(i == 2){
						if((/*stat & 0x0000200*/data[49] & 0x01) != 0){
							data[28] = 1;	//初期値
						}
					} else if(i == 3){
						if((data[49] & 0x20) != 0){
							data[28] = 1;	//初期値
						}
					} else if(i == 4){
						if((data[49] & 0x100) != 0){
							data[28] = 1;	//初期値
						}
					}
					data[29] = 1;	//桁数－１
					strdata[2] = StrList[6][i];
					StrList[14] = StrList[11];
					//容量を稼ぐために消去
					/*
					 strdata[7] = null;
					 StrList[15] = new String[3];
					 StrList[15][0] = "SO製端末を使用";
					 StrList[15][1] = "する場合は○に";
					 StrList[15][2] = "してください。";
					 data[51] -= data[30] * 2;
					 data[53] += data[30] + 5;
					 stat2 |= 0x1000000;
					 */
					//容量を稼ぐために消去
					/*
					 strdata[7] = null;
					 StrList[15] = new String[3];
					 StrList[15][0] = "N504iで不具合";
					 StrList[15][1] = "があるかもしれ";
					 StrList[15][2] = "ません。";
					 data[51] -= data[30] * 2;
					 data[53] += data[30] + 5;
					 stat2 |= 0x1000000;
					 */							}
				break;

				case 5:	//戻る
					stat2 ^= 0x0000004;
					ListBox = StrList[4];
					//data[19] = ListBox.length;
					//data[10] = data[11] = 0;
					data[10] = data[20];	data[11] = data[21];
				break;
			}
	}

	/**
	 * その他の設定メニュー
	 * @param i 選択した箇所
	 */
	public final void othermenu(int i){
		switch(i){
			case 0:	//ﾊﾟｹｯﾄ代の累計
				//strdata[7] = null;
				//StrList[15] = new String[2];
				//StrList[15][0] = "ﾊﾟｹｯﾄ代の累計";
				//StrList[15][1] = "約" + data[47] / 100 + "." + data[47] % 100 + "円";
				//stat2 |= 0x0001000;
				viewcost();
			break;
			case 1:	//累計のﾘｾｯﾄ
				data[47] = 0;
				//SaveSetting();

				strdata[7] = "ﾘｾｯﾄ完了";
				stat2 |= 0x0001000;
			break;
			case 2:	//ﾊﾟｹｯﾄ代の単価
			case 3:	//ﾊﾟｹｯﾄ代警告
			case 4:	//先読み機能
				if((stat & 0x0000400) != 0){	//設定した後
					stat ^= 0x0000400;
					stat2 ^= 0x0000100;	//数字
					if(i == 2){
						data[69] = data[28];
					} else if(i == 3){
						data[91] = data[28];
					} else{
						data[84] = data[28];
					}
	//				data[51] = height / 2;
	//				data[53] = height / 2;
					stat2 ^= 0x1000000;
				} else {
					NumSet();
					if(i == 2){
						data[28] = data[69];	//初期値
						//strdata[7] = null;
						/*StrList[15] = new String[4];
						StrList[15][0] = "単位は";
						StrList[15][1] = "\\/1000pcktです";
						StrList[15][2] = "ex)\\0.3/p->300";
						StrList[15][3] = "0:自動設定";*/
						StrList[15] = StrList[19];
					} else if(i == 3){
						data[26] = 999999;	//上限
						data[29] = 5;	//選択出来る数字の上限の桁数-1

						data[28] = data[91];	//初期値
						//strdata[7] = null;
						/*StrList[15] = new String[3];
						StrList[15][0] = "設定金額を超え";
						StrList[15][1] = "ると､警告が表示";
						StrList[15][2] = "されます｡";*/
						StrList[15] = StrList[20];
					} else {
						data[28] = data[84];	//初期値
						/*StrList[15] = new String[4];
						StrList[15][0] = "ここで指定した";
						StrList[15][1] = "ﾚｽ数前になると";
						StrList[15][2] = "先読みをします";
						StrList[15][3] = "0:先読みしない";*/
						StrList[15] = StrList[21];
					}
	//				data[51] -= data[30] * 2;
	//				data[53] += data[30] + 5;
					stat2 ^= 0x0002000;
					strdata[2] = StrList[8][i];

					stat2 |= 0x1000000;
				}
			break;
			case 5:	//自動しおり機能
			case 6:	//ﾒｰﾙ着信通知機能
			case 7:	//透明削除対策
			case 8:	//最新ﾚｽ表示(ｽﾚ覧)
			case 16://書き込み画面のURL
			case 17://常にアンカーレス
				if((stat & 0x0000400) != 0){	//設定した後
					stat ^= 0x0000400;
					stat2 ^= 0x0000200;	//１文字
					if(i == 5){
						if(data[28] == 1){
							data[57] |= 0x00000200;
						} else {
							data[57] &= ~0x00000200;
						}
					} else if(i == 6){
						if(data[28] == 1){
							data[57] |= 0x00000800;
						} else {
							data[57] &= ~0x00000800;
						}
					} else if(i == 7){
						if(data[28] == 1){
							data[57] |= 0x00002000;
						} else {
							data[57] &= ~0x00002000;
						}
					} else if(i == 8) {
						if(data[28] == 1){
							data[57] |= 0x00010000;
						} else {
							data[57] &= ~0x00010000;
						}
					} else if(i == 17){
						if(data[28] == 1){
							data[57] |= 0x00080000;
						} else {
							data[57] &= ~0x00080000;
						}
					}else {
						data[96] = data[28];
					}
	//				data[51] = height / 2;	data[53] = height / 2;
					stat2 ^= 0x1000000;
				} else {
					//strdata[7] = null;
					//StrList[15] = new String[3];
					stat |= 0x0000400;
					stat2 |= 0x0000200;	//１文字
					data[26] = 1;	//上限
					data[27] = 0;	//桁
					data[28] = 0;	//初期値
					if(i == 5){
						if((data[57] & 0x00000200) != 0){
							data[28] = 1;	//初期値
						}
						/*StrList[15][0] = "ﾚｽを読み込むと";
						StrList[15][1] = "ﾌﾞｯｸﾏｰｸを自動";
						StrList[15][2] = "で更新します。";*/
						StrList[15] = StrList[22];
					} else if(i == 6){
						if((data[57] & 0x00000800) != 0){
							data[28] = 1;	//初期値
						}
						/*StrList[15][0] = "ﾒｰﾙ着信->中断";
						StrList[15][1] = "iappli x504,5i";
						StrList[15][2] = "ez+ phase2.5用";*/
						StrList[15] = StrList[23];
					} else if(i == 7){
						if((data[57] & 0x00002000) != 0){
							data[28] = 1;	//初期値
						}
						/*StrList[15][0] = "透明削除時に";
						StrList[15][1] = "最新ﾚｽを読み";
						StrList[15][2] = "込みます。";*/
						StrList[15] = StrList[24];
					} else if(i == 8) {
						if((data[57] & 0x00010000) != 0){
							data[28] = 1;	//初期値
						}
						StrList[15] = StrList[31];
					} else if(i == 17){
						if((data[57] & 0x00080000) != 0){
							data[28] = 1;	//初期値
						}
						StrList[15] = StrList[38];
					}else {
						data[28] = data[96];	//初期値
						StrList[15] = StrList[33];
					}
					data[29] = 1;	//桁数－１
					strdata[2] = StrList[8][i];
					StrList[14] = StrList[11];
	//				data[51] -= data[30] * 2;	data[53] += data[30] + 5;
					stat2 |= 0x1000000;
				}
			break;

			case 9:	//7ｷｰの機能(ｽﾚ覧)
				stat4 |= 0x0000100;
				ListBox = StrList[2];
				data[19] = data[10];
				data[10] = data[94];
				data[11] = 0;
	//			strdata[7] = "機能を選択してください";
	//			stat2 |= 0x0001000;
			break;

			case 10:	//7ｷｰの機能(ﾌﾞｸﾏ覧)
				stat4 |= 0x0000400;
				ListBox = StrList[17];
				data[19] = data[10];
				data[10] = data[71];
				data[11] = 0;
	//			strdata[7] = "機能を選択してください";
	//			stat2 |= 0x0001000;
			break;

			case 11:	//7ｷｰの機能(ﾚｽ覧)
				stat4 |= 0x0001000;
				ListBox = StrList[3];
				data[19] = data[10];
				data[10] = data[73];
				data[11] = 0;
	//			strdata[7] = "機能を選択してください";
	//			stat2 |= 0x0001000;
			break;

			case 12:	//0ｷｰの機能
				stat4 |= 0x0008000;
				ListBox = StrList[37];
				data[19] = data[10];
				data[10] = data[74];
				data[11] = 0;
	//			strdata[7] = "機能を選択してください";
	//			stat2 |= 0x0001000;
			break;

			case 13:    //ｻｰﾊﾞｰ設定
			case 14:    //拡張ｵﾌﾟｼｮﾝ
			case 15:    //ｸｯｼｮﾝﾘﾝｸ
			case 19:    //NGﾜｰﾄﾞ
			//case 17:   //ﾊﾞｸﾞ報告
				inputForm = new Form(StrList[8][i]);
				inputForm.addCommand(command[4]);
				inputForm.addCommand(command[2]);
				if(i == 13){
					inputForm.append(new StringItem(">現在の接続先",""));
					//inputForm.append(new StringItem(server,""));
					choice = new ChoiceGroup(server+"\n>接続先の変更", Choice.EXCLUSIVE);
					choice.append("zuzu鯖作者版\n("+server_url[0]+")", null);
					choice.append("作者鯖\n("+server_url[1]+")", null);
					choice.append("zuzu鯖独自版\n("+server_url[2]+")", null);
					choice.append("手動設定", null);
					inputForm.append(choice);
					tfield = new LocalizedTextField(">手動設定のｱﾄﾞﾚｽ", server, 300, LocalizedTextField.URL);
					stat2 |= 0x0080000;
					inputForm.append(tfield);
				} else if(i == 14) {
					tfield = new LocalizedTextField("設定", extendedoption, 200, LocalizedTextField.URL);
					stat2 |= 0x10000000;
					inputForm.append(tfield);
				} else if(i == 15) {
					tfield = new LocalizedTextField("ｸｯｼｮﾝのURL", cushionlink, 300, LocalizedTextField.URL);
					tfield2 = new LocalizedTextField("ｸｯｼｮﾝﾘﾝｸの一覧", cushionlinklist, 30000, LocalizedTextField.ANY);
					stat4 |= 0x00004000;
					inputForm.append(tfield);
					inputForm.append(tfield2);
				}else if(i == 18){
					tfield = new LocalizedTextField("NGﾜｰﾄﾞ", "未実装", 4096, LocalizedTextField.ANY);
					stat4 |= 0x00020000;
					inputForm.append(tfield);
				}/* else {
					tfield = new LocalizedTextField("ﾊﾞｸﾞ報告", bagdata, 30000, LocalizedTextField.ANY);
					inputForm.append(tfield);
				}*/
				inputForm.setCommandListener(this);
				disp.setCurrent(inputForm);
			break;

			case 18:	//ｽﾚｯﾄﾞ速度
				if((stat & 0x0000400) != 0){	//設定した後
					stat ^= 0x0000400;
					stat2 ^= 0x0000100;	//数字
					data[82] = data[28];
					stat2 ^= 0x0002000;
				} else {
					NumSet();
					data[28] = data[82];	//初期値
					strdata[2] = StrList[8][i];

				}
			break;

			case 20:
				stat2 &= ~0x0004000;	//function解除

				try{
					/*String buf = "";
					for(i = 0; i < data[85]; i++){
						buf = buf + CacheResData[nCacheIndex][i][0]  + "\n";
					}
					tbox = new LocalizedTextBox("ﾚｽ", buf, buf.length(), LocalizedTextField.ANY);*/
					//System.out.println("\r\n\r\n-------" + resstr + "------\r\n\r\n");
					tbox = new LocalizedTextBox("ﾊﾞｸﾞ報告", bagdata.toString(), bagdata.length() + 1, LocalizedTextField.ANY);
					//inputForm = new Form(StrList[3][i]);
					//inputForm.addCommand(command[4]);
					tbox.addCommand(command[2]);
					stat3 |= 0x0002000;
					//inputForm.append(tfield);
					//inputForm.append(new TextBox("ﾚｽ", buf, buf.length(), TextField.ANY));
					tbox.setCommandListener(this);
					disp.setCurrent(tbox);
				} catch(Exception e){
					stat3 &= ~0x0002000;
					System.gc();
					disp.setCurrent(this);
				}
				stat |= 0x1000000;	//画面更新
			break;

			/*case 18:	//ﾌﾞｯｸﾏｰｸ初期化
				try {
					RecordStore.deleteRecordStore("Bookmark");
				} catch(Exception e){}
				Load();
				strdata[7] = "ﾌﾞｸﾏを初期化完了";//StrList[10][11] + StrList[10][19];
				stat2 |= 0x0001000;
			break;
			*/
			case 21:	//初期化
				try {
					RecordStore.deleteRecordStore("Setting");
					RecordStore.deleteRecordStore("Bookmark");
				} catch(Exception e){}
				Load();
				strdata[7] = "初期化完了";//StrList[10][11] + StrList[10][19];
				stat2 |= 0x0001000;

			break;
			case 22:	//メモリ情報	//ﾒﾓﾘﾏｯﾌﾟ
			/*
				strdata[7] = null;
				StrList[15] = new String[5];
				StrList[15][0] = "Map:sboooooooo";
				StrList[15][1] = "s:system";
				StrList[15][2] = "b:ﾌﾞｯｸﾏｰｸ";
				StrList[15][3] = "x:使用中";
				StrList[15][4] = "o:空き";
				stat2 |= 0x0001000;
			*/
	//			strdata[7] = "工事中m(_ _)m";
	//			stat2 |= 0x0001000;
				System.gc();
				Runtime runtime = Runtime.getRuntime();
				//strdata[7] = null;
				StrList[15] = new String[3];
				StrList[15][0] = "ﾒﾓﾘ情報";
				StrList[15][1] = "free:" + runtime.freeMemory();
				StrList[15][2] = "total:" + runtime.totalMemory();
				stat2 |= 0x0001000;
				//stat |= 0x1000000;	//画面更新
			break;

			case 23:	//戻る

				stat2 ^= 0x0000020;
				ListBox = StrList[4];
				//data[19] = ListBox.length;
				//data[10] = data[11] = 0;
				data[10] = data[20];	data[11] = data[21];
			break;
		}
		stat |= 0x1000000;	//画面更新
	}

	/**
	 * 通信設定メニュー
	 * @param i 選択した箇所
	 */
	private final void networksetting(int i) {
		switch(i){
		//圧縮率の値を変えるときは注意！
		case 0:	//1回に読むｽﾚ数
		case 1:	//1回に読むﾚｽ数
			if((stat & 0x0000400) != 0){	//設定した後
				stat ^= 0x0000400;
				stat2 ^= 0x0000100;	//数字
				data[i] = data[28];
				stat2 ^= 0x0002000;
			} else {
				NumSet();
				data[28] = data[i];	//初期値
				if(i == 2){
					data[26] = 3;	//上限
					data[29] = 0;	//桁数－１
				}
				strdata[2] = StrList[7][i];
			}
		break;

		case 2:		//最新ﾚｽで読む数
		case 3:		//gzip圧縮
		case 4:		//iMona圧縮
		case 9:		//AAの表示
		case 10:	//URLの表示
			if((stat & 0x0000400) != 0){	//設定した後
				stat ^= 0x0000400;
				stat2 &= ~0x0000F00;
				if(i == 2){
					data[55] = data[28];
				} else if(i == 3){
					data[80] = data[28];
				} else if(i == 4){
					data[76] = data[28];
				} else if(i == 9){
					data[87] &= ~0xFF;
					data[87] |= data[28];
				} else {	//i == 10
					data[87] &= ~0xFF00;
					data[87] |= data[28] << 8;
				}
				stat2 &= ~0x0002000;
				data[51] = height / 2;
//				data[53] = height / 2;
				stat2 &= ~0x1000000;
			} else {
				NumSet();
				//stat |= 0x0000400;
				//stat2 |= 0x0002100;	//下二つを結合
				//stat2 |= 0x0000100;	//数字設定
				//stat2 |= 0x0002000;	//数字設定での最も低い数字を1にするフラグ
				data[29] = 0;	//選択出来る数字の上限の桁数-1
				//strdata[7] = null;
				//strdata[7] = null;
				if(i == 2){
					stat2 ^= 0x0002000;		//数字設定での最も低い数字を1にするフラグ
					data[29] = 3;	//選択出来る数字の上限の桁数-1

					data[28] = data[55];	//初期値
					/*StrList[15] = new String[4];
					StrList[15][0] = "0を指定すると";
					StrList[15][1] = "1回に読むﾚｽ数";
					StrList[15][2] = "と同じ値を使用";
					StrList[15][3] = "します。";*/
					StrList[15] = StrList[25];
				} else if(i == 3){
					stat2 ^= 0x0002000;		//数字設定での最も低い数字を1にするフラグ
					data[28] = data[80];	//初期値
					data[26] = 9;	//上限
					/*StrList[15] = new String[4];
					StrList[15][0] = "x505i,japp用";
					StrList[15][1] = "0:圧縮なし";
					StrList[15][2] = "容量:1 > 9";
					StrList[15][3] = "推奨値:6";*/
					StrList[15] = StrList[26];
				} else if(i == 4){
					data[28] = data[76];	//初期値
					data[26] = 3;	//上限
/*									StrList[15] = new String[3];
					StrList[15][0] = "1:非可逆のみ";
					StrList[15][1] = "2:未ｻﾎﾟｰﾄ";
					StrList[15][2] = "3:圧縮なし";
*/
					StrList[15] = StrList[27];
				} else if(i == 9){
					//if((data[57] & 0x00000002) != 0){data[28] = 1;}
					data[28] = data[87] & 0xFF;
					data[26] = 5;	//上限

					/*StrList[15] = new String[5];
					StrList[15][0] = "1:全部表示する";
					StrList[15][1] = "2:AAのみ消去";
					StrList[15][2] = "3:AAのみ消去2";
					StrList[15][3] = "4:2+全消去";
					StrList[15][4] = "5:3+〃";*/
					StrList[15] = StrList[28];
				} else {	//i == 10
					data[28] = (data[87] & 0xFF00) >> 8;
					//if((data[57] & 0x00004000) != 0){
					//	data[28] = 2;
					//} else if((data[57] & 0x00000004) != 0){
					//	data[28] = 1;
					//}

					data[26] = 4;	//上限

					/*StrList[15] = new String[4];
					StrList[15][0] = "1:全部表示する";
					StrList[15][1] = "2:省略する";
					StrList[15][2] = "3:2ch内のみ表示";
					StrList[15][3] = "4:完全消去";*/
					StrList[15] = StrList[29];
				}
				//data[51] -= data[30] * 2;
				data[51] -= /*data[30] * 2 + */data[33];
//				data[53] += data[30] + 5;
				stat2 |= 0x1000000;
				strdata[2] = StrList[7][i];
			}
		break;
		//この4つの値を変えるときは注意！
		case 5:	//IDの表示
		case 6:	//時刻の表示
		case 7:	//ﾒｱﾄﾞの表示
		case 8:	//名前の表示
			//i = data[10] + data[11];

			if((stat & 0x0000400) != 0){	//設定した後
				stat ^= 0x0000400;
				stat2 &= ~0x0000F00;
				//if(i == 9){
				//	data[28] = data[87];
				//} else
				if(data[28] == 1){
					if(i == 5){
						data[49] |= 0x04;
					} else if(i == 6){
						data[49] |= 0x08;
					} else if(i == 7){
						data[49] |= 0x10;
					} else if(i == 8){
						data[49] &= ~0x40;
					//} else if(i == 9){
					//	data[57] |= 0x00000002;
					//} else if(i == 10){
					//	data[57] &= ~0x00004000;
					//	data[57] |= 0x00000004;
					}
				//} else if(data[28] == 2) {
				//	data[57] |= 0x00004000;
				} else {	//data[28] == 0
					if(i == 5){
						data[49] &= ~0x04;
					} else if(i == 6){
						data[49] &= ~0x08;
					} else if(i == 7){
						data[49] &= ~0x10;
					} else if(i == 8){
						data[49] |= 0x40;
					//} else if(i == 9){
					//	data[57] &= ~0x00000002;
					//} else if(i == 10){
					//	data[57] &= ~0x00004004;
					}
				}

//				data[51] = height / 2;
//				data[53] = height / 2;
				stat2 &= ~0x1000000;
			} else {
				stat |= 0x0000400;
				stat2 |= 0x0000200;	//１文字
				data[26] = 1;	//上限
				data[27] = 0;	//桁

				data[28] = 0;	//初期値
				data[29] = 1;	//桁数－１
				if(i == 5){
					if((data[49] & 0x04) != 0){data[28] = 1;}
				} else if(i == 6){
					if((data[49] & 0x08) != 0){data[28] = 1;}
				} else if(i == 7){
					if((data[49] & 0x10) != 0){data[28] = 1;}
				} else if(i == 8){
					if((data[49] & 0x40) == 0){data[28] = 1;}

				}
				strdata[2] = StrList[7][i];
				StrList[14] = StrList[11];
			}
		break;

		case 11:	//戻る
			stat2 ^= 0x0000008;
			ListBox = StrList[4];
			//data[19] = ListBox.length;
			//data[10] = data[11] = 0;
			data[10] = data[20];	data[11] = data[21];
		break;
		}
	}

	/**
	 * 0キーショートカット用の隠しメニュー
	 * @param i 選択した箇所
	 */
	private final void orezimenu(int i) {
		stat4 |= 0x0010000;
		switch(i){
			case 0:	//文字サイズ変更(常)
				if(data[36] == 0){
					data[36] = 1;
				}else if(data[36] == 1){
					data[36] = 2;
				}else if(data[36] == 2){
					data[36] = 0;
				}
				SetFont();
				if((stat & 0x40000) != 0){	//レス表示中
					makeRes(/*0*/);
				}
				//System.out.println("文字ｻｲｽﾞ");
			break;
			case 1: //ｸｯｼｮﾝﾘﾝｸ非使用ｼﾞｬﾝﾌﾟ(ﾚｽ)
				if( (stat & 0x40000) != 0 ){	//レスの表示
					Link(null, 0);
				}
				//System.out.println("非ｸｯｼｮﾝﾘﾝｸｼﾞｬﾝﾌﾟ");
			break;
			case 2://自動しおりONtoOFF
				if((data[57] & 0x00000200) != 0){
					data[57] ^= 0x00000200;
					strdata[7] = "自動しおりOFF";
				}else{
					data[57] |= 0x00000200;
					strdata[7] = "自動しおりON";
				}
				stat2 |= 0x0001000;
			break;
			case 3:
				if( (stat & 0x40000) != 0 ){	//ﾚｽ表示時
					if(-1 == EditBookMark( 0, CacheTitle[nCacheIndex]/*strdata[9]*//*ThreadName[data[4]]*/, nCacheBrd[nCacheIndex]/*data[3]*/, nCacheTh[nCacheIndex][0]/*data[2]*//*nThread[data[4]]*/, data[6]+nCacheSt[nCacheIndex]/*data[7]*/)){
						//strdata[7] = null;
						//StrList[15] = new String[3];
						//StrList[15][0] = "= 登録失敗 =";
						//StrList[15][1] = "ﾌﾞｯｸﾏｰｸがいっ";
						//StrList[15][2] = "ぱいです。";
						strdata[7] = "登録失敗";
						stat2 |= 0x0001000;
					}
				}else if((stat & 0x0010000) != 0){//ｽﾚ表示時
					//if(-1 == EditBookMark( 0, CacheBrdData[nCacheIndex]/*ThreadName*/[data[4]], nCacheBrd[nCacheIndex]/*data[3]*/, nCacheTh[nCacheIndex]/*nThread*//*nThread*/[data[4]], 0)){
					if(-1 == EditBookMark( 0, CacheBrdData[nCacheIndex]/*ThreadName*/[data[60]], nCacheBrd[nCacheIndex]/*data[3]*/, nCacheTh[nCacheIndex]/*nThread*//*nThread*/[data[60]], 0)){
						//strdata[7] = null;
						//StrList[15] = new String[3];
						//StrList[15][0] = "= 登録失敗 =";
						//StrList[15][1] = "ﾌﾞｯｸﾏｰｸがいっ";
						//StrList[15][2] = "ぱいです。";
						strdata[7] = "登録失敗";
						stat2 |= 0x0001000;
					}
				}else if( (stat & 0x0004000) != 0 ){	//板リストにいるとき
					EditBookMark( 0,"[板]" + ListBox[data[10]+data[11]], ((data[22] + data[23]) * 100 + data[10] + data[11]), 0, 0);
				}
			break;
			case 4:
				httpinit(0,0,0,0,0);//dlbrdlist();
			break;
		}
		stat |= 0x1000000;	//画面更新
		stat4 ^= 0x0010000;
	}

	/**
	 * zuzuが追加したログ用処理。
	 * @param text 表示したいテキスト。
	 */
	private final void Bugln(String text) {
		if(bagdata.length() >= 4096 || bagdata.length() <= 0){
			bagdata = null;
			bagdata = new StringBuffer("");
			bagdata.append("Connect:" + server + "\nVersion:" + version + "\n");
			System.out.print("Connect:" + server + "\nVersion:" + version + "\n");
		}
		bagdata.append(text);
		System.out.print(text);
	}
	private final void Bugln(String text,int mode) {
		if(bagdata.length() >= 4096 || bagdata.length() <= 0){
			bagdata = null;
			bagdata = new StringBuffer("");
			bagdata.append("Connect:" + server + "\nVersion:" + version + "\n");
			System.out.print("Connect:" + server + "\nVersion:" + version + "\n");
		}
		
		if(mode == 1){
			//System.out.print(text);
		}else{
			bagdata.append(text);
		}
	}
	private final void allprintStackTrace(Exception e) {
		//e.printStackTrace();
		//Bugln(System.out.toString() + "\n");
	}
	


} //class MainCanvasの終わり