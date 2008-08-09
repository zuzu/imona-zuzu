#!/usr/local/bin/perl

###################################################################################################

#
# irss.pl
#		// iMona RSS 配信スクリプト
#		// [charset:Shift-JIS]
#

###################################################################################################

#【 概要 】
#	このスクリプトは 2ch のスレッドを RSS で取得するためのスクリプトです。
#	XML::RSS モジュールと iMona の dat キャッシュ機能を利用しています。
#	現バージョンではエラー処理を全く行っていませんし、改良余地も多くあります。
#
#【 呼び出し方法 】
#	http://～/irss.pl/AAA.2ch.net/BOARDNAME/ => 未対応
#	http://～/irss.pl/AAA.2ch.net/test/read.cgi/BOARDNAME/1234567890/
#	http://～/irss.pl/AAA.2ch.net/test/read.cgi/BOARDNAME/1234567890/l50
#	http://～/irss.pl/AAA.2ch.net/BOARDNAME/1234567890/123-456
#	など適当に。
#
#【 更新履歴 】
#	06/05/03 ver0.1
#	・初公開

###################################################################################################

BEGIN {	# 初回起動時のみ
	use XML::RSS;
	require '2c.pl';
	
	$maxres = 9999;	#最大レス読み込み数制限
}

print "Content-Type: text/xml\n\n";
&read();

exit();


sub read {
	$/ = "\x0A";	#改行コードを\x0A(LF)にする。
	binmode(STDOUT);

	$path = $ENV{PATH_INFO};

	# サーバの取得
	$path =~ s/^\/?([^\/]*)\//\//;
	$server = $1;

	# いらない物を削除
	$path =~ s|/i/|/|g;		$path =~ s|/read.cgi/|/|g;
	$path =~ s|/p.i/|/|g;	$path =~ s|/r.i/|/|g;
	$path =~ s|/test/|/|g;

	# スレッド番号を取得
	if($path =~ /([0-9]{9,10})/) {	#レスの表示
		$ithread = $1;
		$path =~ s|/([0-9]{9,10})/|/|;
	}
	$path =~ s|//+|/|g;

	# 板名を取得
	if($path =~ /\/([A-Za-z0-9]{2,10})\// ){
		$board = $1;
	} else {	#板名が取得できない
		#print "エラーです<br>板の名前が取得できませんでした<br>処理しようとしたデータ：$_path<br>現在のデータ：$path";
		return;
	}

	if($ithread != 0){	# レスの表示
		if($path =~ /\/(l?)([0-9]+)(\-?)([0-9]*)$/){	# レス位置指定がある場合
			if($1 ne ''){#last
				$last = $2;
				if($last > $maxres){$last = $maxres;}
				$op = 'f';
			} else {
				if($3 ne ''){#-
					$start = $2;
					if($4 eq '' || $4 - $2 > $maxres){
						$to = $start + $maxres;	$op = 'f';
					} else {
						$to = $4;
					}
				} else {	#1レスを指定
					$start = $2;
					$to = $2;
				}
			}
		} else {										# 無い場合
			$start = 1;	$to = $maxres; $op = 'f';
		}
		if($path =~ /\/[\w\-]*n[\w\-]*$/){	# no first
			$op =~ s/f//;
		}

		# 2ch またはキャッシュからデータの取得
		$data = &p2chcache'read($server, $board, $ithread, $start, $to, $last, $op);
		@data = split(/\n/, $data);

		# 出力
		putresrss();
	} else {	# 板の表示
		#print $data;
	}
}

sub putresrss {
	
	# ヘッダ部分の解析
	my $title = (split(/<>/, $data[1]))[4];
	my ($st, $end, $all);
	$data[0] =~ m/Res:([0-9]*)\-([0-9]*)\/([0-9]*)/i;	#始め-終わり/全部の数
	$st = $1;	$end = $2; $all = $3;
	if($end > $all){$end = $all;}

	# XML モジュールの初期化
	my $rss = new XML::RSS (version => '1.0', encoding => 'Shift_JIS');
	$rss->add_module(prefix => "content",
		uri => "http://purl.org/rss/1.0/modules/content/"
	);

	# 表示する RSS のリソース URL の決定
	$url = $ENV{PATH_INFO};
	$url =~ s|^.+/([^/]+?)$|$1|;
	$url = "http://" . $server . "/test/read.cgi/" . $board . "/" . $ithread . "/" . $url;

	$rss->channel(
		title        => $title,
		link         => $url,
		description  => $title,
	);
	
	if($st == 0){
		#print "表\示するレスがありません<br><br>(dat落ち、レス指定範囲エラー、バグなど)";
		$end = $start - 1;
	} else {
		for($i = 0; $i <= ($end - $st) + 1; $i++){
			if($op ne ''){
				if($i == 0){
					$resnum = 1;
				} elsif($st == 1) {
					$resnum = $st+$i;
				} else {
					$resnum = $st+$i-1;
				}
			} else {
				$resnum = $st+$i;
			}
			if($data[$i+1] eq ''){next;}
			@res = split(/<>/, $data[$i+1]);

			# タイトル(投稿者部分の処理)
			$title = $resnum . " ：$res[0]";
			if($res[1] ne ''){ $title .= " [$res[1]]"; }	# メール欄
			$title .= " $res[2]";							# 時刻など
			$title =~ s/<\/?b>//g;

			# 表示する RSS のリソース URL の決定
			$url = "http://" . $server . "/test/read.cgi/" . $board . "/" . $ithread . "/" . $resnum;

			$description = $res[3];
			$description =~ s/<br>/ /g;
			$description =~ s/<a [^>]+?>(.+?)<\/a>/$1/g;

			$content = $res[3];
			$content =~ s|<a href="../test/read.cgi|<a href="http://$server/test/read.cgi|g;
			
			$rss->add_item(
				title       => $title,
				link        => $url,
				description => $description,
				content => {
					'encoded' => "<![CDATA[$content]]>"
				},
			);

		}
	}
	print $rss->as_string;
}
