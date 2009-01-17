<?php
//変数宣言
$url = array();
$data = array();
$brd4_top = array();
$brd4_under = array();
$brd5 = array();
$brdflex = array();
$num = -1;
$tab = 0;
$skipflg = 0;
$hit = 0;
$brdflexnum[0] = 0;
$brdflexnum[1] = 0;

##/設定部分/################################################
//#URLリスト(上から順に処理していきます)
$url = array("http://imona.zuzu-service.net/bbsmenu.html",
"http://menu.2ch.net/bbsmenu.html");
//無視リスト
$skip = array("特別企画","2chプロジェクト","2NN+","いろいろランク","チャット","運営案内","ツール類","STOP","2ch総合案内","2chの入り口","会議室","TOPページ");
//許可アドレスリスト(正規表現使用)
$addsitelist = array("2ch\.net","bbspink\.com","kakiko\.com","pf-x\.net","newsplus\.jp","livedoor\.jp","vip2ch\.com","2ch2\.net","3ch\.jp","[0-9]+?\.kg","k2y\.info","psychedance\.com","xrea\.com","machi\.to","zuzu\-service\.net","nicovideo\.jp","ktkr\.net","shiroro\.com");
//停止位置
$last = '<BR><BR><B>他のサイト</B><BR>';
$brd4path = "brd4.txt";
$brd5path = "brd5.txt";
$brdflexpath = "brdflex.txt";
//結果を表示するかしないか(true,false)
$output = true;
############################################################

//配列の変換
$addsite = implode("|", $addsitelist);

//URLの読込
foreach( $url as $_ ){
	$handle = fopen ($_, "r");
	while (!feof ($handle)) {
		array_push($data,fgets($handle, 4096));
	}
}
//実際の更新処理
//の前に文字コード変換_SJIS->UTF8
//$data = mb_convert_variables('UTF-8', 'SJIS', $data);
foreach( $data as $_ ){
	$_ = mb_convert_encoding($_, "UTF-8", "SJIS");
	
	if (stristr($_, $last) != FALSE) { break; }
	
	$skipflg = 0;
	foreach( $skip as $line ){
		if(stristr($_, $line) != FALSE){ $skipflg = 1;break; }
	}
	
	/*if($skipflg == 1){
		 next;
		// echo $_;
	}*/
	if($skipflg != 1){
		if ( preg_match ( "/"."<BR><BR><B>(.+?)<\/B><BR>"."/i", $_, $match ) ) {
			$skipflg = 0;
			array_push($brd4_top,$match[1]);
			if($hit == 1){
				$brd4_under[$num] = trim($brd4_under[$num])."\n";
				$brd5[$num] = trim($brd5[$num])."\n";
				$brdflex[$num] = trim($brdflex[$num])."\n";
				$num = $num + 1;
				$brdflexnum[0] = $brdflexnum[0] + 1;
				$brdflexnum[1] = 0;
				$hit = 0;
			}
		}
		if ( preg_match ( "/"."<A HREF=(http:\/\/.+?\.($addsite)\/.+?\/)( TARGET=_blank)?>(.+)<\/A>"."/i", $_, $match ) ) {
			$skipflg = 0;
			$hit = 1;
			$brd4_under[$num] = $brd4_under[$num].$match[4]."\t";
			$brd5[$num] = $brd5[$num].$match[1]."\t";
			if($brdflexnum[0] >=10){
				if($brdflexnum[1] >= 10){
					$brdflex[$num] = $brdflex[$num].$brdflexnum[0].$brdflexnum[1]."\t";
				}else{
					$brdflex[$num] = $brdflex[$num].$brdflexnum[0].'0'.$brdflexnum[1]."\t";
				}
			}elseif($brdflexnum[0] == 0){
				$brdflex[$num] = $brdflex[$num].$brdflexnum[1]."\t";
			}else{
				if($brdflexnum[1] >= 10){
					$brdflex[$num] = $brdflex[$num].$brdflexnum[0].$brdflexnum[1]."\t";
				}else{
					$brdflex[$num] = $brdflex[$num].$brdflexnum[0].'0'.$brdflexnum[1]."\t";
				}
			}
			$brdflexnum[1] = $brdflexnum[1] + 1;
		}
	}
}
//最終行のトリム
$brd4_under[$num] = trim($brd4_under[$num]);
$brd5[$num] = trim($brd5[$num]);
$brdflex[$num] = trim($brdflex[$num]);

//brbflexのバグ対策
//$brd4_under[$num] = $brd4_under[$num]."\n";

//保存処理&表示処理
if($output){
	echo mb_convert_encoding("<hr><font color='green'># カテゴリや板の名前</font>", "SJIS", "UTF-8");;
	echo "<hr><pre>\n";
}

$fp = fopen($brd4path,"w");
$fp2 = fopen($brdflexpath,"w");
$brd4_toptext = implode("\n", $brd4_top);
$brd4_toptext = mb_convert_encoding($brd4_toptext, 'SJIS', 'UTF-8');
if($output){
	echo $brd4_toptext;
}
fwrite($fp,$brd4_toptext);
fwrite($fp2,$brd4_toptext);
/*unset $brd4_toptext;
unset $brd4_top;*/
/*
foreach( $brd4_top as $_ ){
	if($output){
		echo "$_\n";
	}
	$_ = mb_convert_encoding($_, "SJIS", "UTF-8");
	fwrite($fp,"$_\n");
	fwrite($fp2,"$_\n");
}
if($output){
	echo "\n";
}*/
fwrite($fp,"\n\n");
fwrite($fp2,"\n\n");
$count = -1;

//$brd4_under = mb_convert_variables('SJIS', 'UTF-8', $brd4_under);
$brd4_undertext = implode("", $brd4_under);
$brd4_undertext = mb_convert_encoding($brd4_undertext, 'SJIS', 'UTF-8');
fwrite($fp,$brd4_undertext);
fclose($fp);
if($output){
	echo "<br><br>";
	echo $brd4_undertext;
}
//unset $brd4_undertext;

foreach( $brd4_under as $_ ){
	$_ = mb_convert_encoding($_, "SJIS", "UTF-8");
	if( !strpos($_,"\n") ) {
		fwrite($fp2,$_."\n");
	}else{
		fwrite($fp2,$_);
	}
	fwrite($fp2,$brdflex[$count]);
	$count = $count + 1;
}
fclose($fp2);
/*
foreach( $brd4_under as $_ ){
	if($output){
		echo $_;
	}
	$_ = mb_convert_encoding($_, "SJIS", "UTF-8");
	fwrite($fp,$_);
	if( !strpos($_,"\n") ) {
		fwrite($fp2,$_."\n");
	}else{
		fwrite($fp2,$_);
	}
	fwrite($fp2,$brdflex[$count]);
	$count = $count + 1;
}
fclose($fp2);
*/

if($output){
	echo "</pre>\n";
	echo mb_convert_encoding("<hr><font color='green'># 板番号－＞URL変換用</font>", "SJIS", "UTF-8");;
	echo "<hr><pre>\n";
}

$fp = fopen($brd5path,"w");
$brd5text = implode("", $brd5);
$brd5text = mb_convert_encoding($brd5text, "SJIS", "UTF-8");
if($output){
	echo $brd5text;
}
fwrite($fp,$brd5text);
/*
foreach( $brd5 as $_ ){
	if($output){
		echo "$_";
	}
	$_ = mb_convert_encoding($_, "SJIS", "UTF-8");
	fwrite($fp,"$_");
}*/
fclose($fp);
if($output){
	echo "</pre><hr>\n";
}
/*foreach( $brdflex as $_ ){
	$_ = mb_convert_encoding($_, "SJIS", "UTF-8");
	fwrite($fp,"$_\n");
}
fclose($fp);*/

exit;

?>