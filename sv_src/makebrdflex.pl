#!/usr/local/bin/perl

# ・新規板の自動作成
# ・移転自動対応
# ・brdflex.txt生成
# を行うスクリプト
#
# 文字コード EUC
# 変更不可
#
# make brdflex.txt
#         soft.spdv.net

##/設定部分/########################
$brd2 = 'brd2.txt';	# 板データ(board.cgiで作成したもの) カテゴリや板の名前
$brd3 = 'brd3.txt';	# 板データ(board.cgiで作成したもの) 板番号−＞URL変換用
$brd4 = 'brd4.txt';	# 新しい板データ(board.cgiで作成したもの) カテゴリや板の名前
$brd5 = 'brd5.txt';	# 新しい板データ(board.cgiで作成したもの) 板番号−＞URL変換用
$flexiblebrd = 'brdflex.txt';	# ver15以上で使用する板一覧データ 端末に出力する

$ignore = " info.2ch.net epg.2ch.net movie.2ch.net watch.2ch.net ";		# 無視するサーバ
$ignorecategory = " チャット 運営案内 ツール類 まちＢＢＳ 他のサイト ";	# 無視するカテゴリ
$lastcategory = "まちＢＢＳ";											# 最終カテゴリ

$addboard = 1;	# 新規板を自動で追加する
####################################

require 'editbrd.pl';
require 'http.pl';
require 'jcode.pl';


#初期化
$http'ua = $ua;
$http'range = 0;
$http'other = '';

@category = ();
@brd = ();
$brdtmp = "";

$str = &http'get("http://menu.2ch.net/bbsmenu.html");		#ダウンロード

$str =~ s/\r\n/\n/;

&jcode'sjis2euc(*str, 'h'); # eucに変換+全角カナを半角カナに変換
&jcode'tr(*str, '０-９Ａ-Ｚａ-ｚ”＃＄％＆’｜￥＾！　（）｛｝［］：；＋＊＝＜＞？／＿＠−', '0-9A-Za-z"#$%&\'|\\^! (){}[]:;+*=<>?/_@-');	# 以下２つを合体
&jcode'euc2sjis(*str); # sjisに変換

&jcode'euc2sjis(*ignorecategory); # sjisに変換
&jcode'euc2sjis(*lastcategory); # sjisに変換

@data = split(/\n/, $str);

&peditbrd'cachebrd5();

$skipcategory = 0;
foreach $tmp (@data){
	if($tmp =~ /<BR><BR><B>(.+?)<\/B><(BR|br)>/){	#category
		$skipcategory = 0;
		if($brdtmp ne ""){
			$brdtmp =~ s/\t$//;
			$nbrdtmp =~ s/\t$//;
			push(@category, $cattmp);
			push(@brd, $brdtmp);
			push(@brd, $nbrdtmp);
			$brdtmp = "";
			$nbrdtmp = "";
		}

		$cattmp = $1;
		if($ignorecategory =~ / $cattmp /){$skipcategory = 1;	next;}
		if($cattmp eq $lastcategory){last;}
	} elsif($skipcategory == 0 && $tmp =~ /<A HREF=http:\/\/((.+?)(\/.+?\/))>(.+?)<\/A>/) {
		$url = $1;
		$server = $2;
		$bdir = $3;
		$bname = $4;
		if($server !~ /2ch\.net/ && $server !~ /bbspink\.com/) {next;}
		if($ignore !~ / $server /){	#無視リストに入っていない
			$nbrd = &peditbrd'url2nbrd($bdir);
			if($nbrd == -1){
				print "Brd Not Registered $cattmp $tmp";
				if(&peditbrd'makebrd($cattmp, $bname, "http://" . $url) == 1){
					print " ... automatically generated! please run again\n";
				} else {
					print "\n";
				}
			} else {
				$transerver = &peditbrd'sbrd2server($bdir);

				# 板が移転している時
				if($transerver ne $server) {
					print "Server transfered $cattmp $tmp from: $transerver to $server";

					# 移転
					if (&peditbrd'transfer("http://" . $url) == 1) {
						print " ... automatically transfered! please run again\n";
					} else {
						print "\n";
					}
				}
				
				$brdtmp .= "$bname\t";
				$nbrdtmp .= $nbrd . "\t";
			}
		}
	}
}

if(open(FILE, "+< $flexiblebrd")){
	binmode(FILE);
	flock(FILE, 2);				# ロック確認。ロック
	seek(FILE, 0, 0);			# ファイルポインタを先頭にセット

	print FILE join("\n", @category);
	print FILE "\n\n";
	print FILE join("\n", @brd);

	truncate(FILE, tell(FILE));	# ファイルサイズを書き込んだサイズにする
	close(FILE);
} else {
	open(FILE, "> $flexiblebrd");
	binmode(FILE);
	print FILE join("\n", @category);
	print FILE "\n\n";
	print FILE join("\n", @brd);
	close(FILE);
}

