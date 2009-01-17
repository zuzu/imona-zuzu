#!/usr/local/bin/perl

# 2ch Cache system
#         soft.spdv.net

#★これは何？
# 2chのread.cgiの代わりをして、2chの負荷を下げるスクリプトです。
# dat読みを行い、一度読み込んだ物はキャッシュされるので、
# キャッシュサーバーとして運用することができます。
# ネットワーク越しでも、ローカルでも使用できます。

#★使用方法
# 1.CGIで呼び出すとき
# 呼び出し例
# /2c.pl?host=BBB.2ch.net&bbs=AAA&th=123456789&st=1&to=100&op=f
# lsも使用可能です。両方存在する場合は、lsが優先されます。
# また、POST形式でのリクエストも使用できます。
#
# 2.require '2c.pl';で使用するとき
# &p2chcache'read(HOST, BOARD, THREAD, START, END(TO), LAST, OPTION);
# 使用例
# &p2chcache'read('BBB.2ch.net', 'AAA', '123456789', '1', '100', '0', 'f');
# STARTとENDは、LAST=0の時に使用されます。

# オプションについて
# f:first 1番目のレスを同時に出力します

# それぞれ、THREAD欄が無い場合は板一覧の呼び出しと判断します。
# 出力されるデータはrawmodeから出力するデータを減らしたような感じです(Res:123-456/789の部分しか出力しません)。
# また、レスの読み込みの場合、先頭のレスの最後に必ずスレッドのタイトルが入るようになっています。
# これは、iMonaではURLのみでレスを読もうとしたときに使用されます。

#★仕様
# 初めて読み込むスレッドはdat読みを行います。あぼーん、透明あぼーん時にサイズ変更のチェックが完全に出来そうにないので、
# 差分読み込みはIf-Modified-Sinceを使用し、更新されていた場合はすべてのデータを再びダウンロードします。

# エラーはなるべく出さないようにしていますが、指定された範囲のレスが存在しない場合、
# または通信に失敗した場合はr.iのようにERR - XXX エラー文字列
# といったデータが１行目に出力されます。２行目以降は何もありません。
# XXXに何が入るのかはまだ決まっていません。とりあえず、現在はすべてERR - 400と出力しています。

# 保存されるDATの仕様は、ファイルの１行目にLast-Modified\tデータの行数(\tはタブ)
# が保存されてあり、２行目からは本来の*.dat,suject.txtのデータと同じものが保存されています。
# 改行コードは\n(0x0A,\x0A)を使用します。

# datの差分読み込みは、現在取得しているレスの最後を含む範囲を読み込み、
# 今ある最後のレスと読み込んだ最初のレスが一致していた場合(あぼーんなどはおそらくされていない)に更新します。
# 一致しなかった場合は、datのはじめから読み直します。
# この方法ではほとんどないと思いますがdatが壊れる可能性もあります。
# 例えば二重カキコし、そのカキコと同じサイズのレスが透明あぼーんされた場合などです。詳細は割愛します。

#★過去ログを読むとき
# 「\dat\該当する板名\」にdatファイルをそのまま放り込んでください。
# すると通常の方法(放り込んだ板の板番号とスレ番号)でスレを読むことができます。

#★更新履歴
#08/09/19 ver5.3
#・スレッドの速度によってキャッシュの更新間隔を調整する機能の追加
#08/07/19 ver5.2
#・bg20を使用したスレッド取得方法の追加
#07/06/11 ver5.1
#・次スレ検出機能の精度向上
#06/10/24 ver5.0
#・次スレ検出機能の追加
#06/05/08 ver4.3
#・巡回用高速スレッド更新チェック機能の追加(Alpha Version)
#06/01/23 ver4.2
#・subject.txtの最大キャッシュ使用時間を規定することによって移転の自動追随に失敗することがあったのを修正した。
#05/04/16 ver4.1
#・クロール規制に対応(エラーの変更、キャッシュ読み込みの強化)
#05/02/08 ver4.0
#・dat逆読みの対応によりHDDアクセスの軽減
#・dat破壊検知の強化
#04/02/17 ver3.0
#・板移転の自動追尾に対応
#04/02/10 ver2.7
#・ファイルサイズの大きいキャッシュを読み書きするときの効率が向上した
#03/11/12 ver2.6
#・0バイトのファイルが作成されないようにした
#03/10/27 ver2.5
#・レス番号の指定がおかしいときエラーと最新レスを返すようにした
#03/10/24 ver2.4
#・dat落ちでもキャッシュしている分は読めるようにした
#03/10/12 ver2.3
#・datの破損に強くなった
#03/10/11 ver2.2
#・2chが落ちているときでもエラーを出力せずにキャッシュを表示するようにした
#03/07/20 ver2.1
#・datの差分読み込みが失敗するバグを修正
#・スレッド一覧が更新されないバグを修正
#・2ch以外の板を格納するディレクトリを変更(\dat\ホスト名\板名\)
#03/07/05 ver2.0
#・datの差分読み込みのサポート。datファイルの仕様上、100%確実でなくdatが壊れる可能性もある
#・上記に関連して少し負荷があがった
#03/06/14 ver1.5
#・普通のdatファイルを過去ログとして使えるようになった(readonly)
#・バグ取り
#03/03/02 ver1.0

package machibbs;

#$ENV{'TZ'} = "JST-9";

BEGIN {	#初回起動時のみ
	## 設定 ###########################################################################################
	do 'setting.pl';
	$ua = 'Monazilla/1.00 (iMona/1.0)';	#USER-AGENT
	$debug = 0;	# debug output
	###################################################################################################

	if(exists $ENV{MOD_PERL}){	#mod_perlで動作しているとき
		require 'http.pl';
	}
	require 'mhttp.pl';

}

if(caller() eq ''){	#requireで呼ばれたのではない場合
	if($mode != 1){
		&decode;
		binmode(STDOUT);
		print "Content-type: text/plain\n\n";
		print &read($FORM{'host'}, $FORM{'bbs'}, $FORM{'th'}, $FORM{'st'}, $FORM{'to'}, $FORM{'ls'}, $FORM{'op'});
	}
	exit();
}

sub read {			# スレ一覧、スレッドの読み込み

	$/ = "\x0A";	#改行コードを\x0A(LF)にする。

	$rhost = $_[0], $rbbs = $_[1], $rth = $_[2], $rst = $_[3], $rto = $_[4], $rls = $_[5], $rop = $_[6];
	$readcache = 0, $head = '', $buffer = '', $outmode = '';
	@data = ();

	if($rst > $rto){	#startの方がtoよりも大きい場合
		$rto = 1024;	#最後まで読む
	}

	&getdir();

	if($rth == 0){	#スレ一覧の読み込み
		$interval = time() - (stat("$dir/$rbbs/subject.txt"))[9];

		&openfile("$dir/$rbbs/subject.txt");

		$head = <RW>;
		@data2 = split(/\t/ , $head);
		chomp($data2[1]);

		if($interval > $brmininterval){	#スレ一覧を再読込する場合
			if(!exists $ENV{MOD_PERL}){	#!mod_perl
				require 'http.pl';
			}
			$http'ua = $ua;
			$http'range = 0;
			$http'other = '';

			if($bruseims == 1){		#更新チェックを使用する
				if($data2[0] ne '' && $data2[1] > 1 && $brmaxinterval > $interval){	#Last-Modified欄が存在し、更新間隔が長すぎない場合
					$http'other = "If-Modified-Since: $data2[0]\r\n\r\n";
				}
			}

			if($readcache != 1){
				$reload = 0;
RELOADBRD:
				$_usebg20 = $usebg20;
				if($rhost !~ /\.2ch\.net/) { $_usebg20 = 0; }
				$str = &mhttp'get("$rhost/bbs/offlaw.cgi/$rbbs/",1);		#ダウンロード

				if($_usebg20 != 1 && $mhttp'header[0] =~ / 304 /){	#キャッシュを使用する(304 Not Modified)
					$readcache = 1;
				#板移転
				# _usebg20 != 1 => 302 Found or (200 OK && len of $str < 100)
				# _usebg20 == 1 => 200 OK && len of $str < 1000 && $str !~ /<>/
				} elsif(($_usebg20 != 1 && $reload == 0 && ($mhttp'header[0] =~ / 302 / || ($mhttp'header[0] =~ / 20[0-9] / && length($str) <= 100))) ||
						($_usebg20 == 1 && $reload == 0 && ($mhttp'header[0] =~ / 20[0-9] / && length($str) <= 500 && $str =~ /ERROR . 5656/))
						)
				{
					foreach $str (@mhttp'header) {
						if($str =~ /^Location: .+\/_?403\//){	# 規制されていた時
							#キャッシュを読むことにする
							$readcache = 1;
						}
					}

					if($readcache != 1){
						$http'range = 0;
						$http'other = '';
						$str = &http'get("$rhost/$rbbs/");		#ダウンロード

						if($str =~ m/window\.location\.href\=\"([^"]+)\"<\/script>/) {
							require 'editbrd.pl';

							&peditbrd'transfer($1);
							$1 =~ m|http://([^/]*)/|;
							$rhost = $1;
							$reload = 1;
							goto RELOADBRD;
						} else {
							if($#data < 0){	#キャッシュが存在しない場合
								return &puterror(400);
							} else {
								#キャッシュを読むことにする
								$readcache = 1;
							}
						}
					}
				} elsif($mhttp'header[0] =~ / 20[0-9] /){				#正常に取得できた場合
					if(!-e "$dir"){mkdir("$dir", $dirpermission);}	#ディレクトリがなければ作成する
					if(!-e "$dir/$rbbs"){mkdir("$dir/$rbbs", $dirpermission);}	#ディレクトリがなければ作成する

					@data = split(/(?<=\n)/, $str);
					if($#data < 0){	#ダウンロードエラー
						return &puterror(400);
					} else {
						if(!-e "$dir/$rbbs/subject.txt"){&createfile("$dir/$rbbs/subject.txt");}			#ファイルがなければ作成する
						seek(RW, 0, 0);			# ファイルポインタを先頭にセット
						print RW getlastmodified(@mhttp'header) . "\t" . ($#data + 1) . "\n" . $str;	#Last-Modified、データの行数
						truncate(RW, tell(RW));
					}
				} else {	#正常に取得できなかったときは保存しないで終了
					if($#data < 0){	#キャッシュが存在しない場合
						return &puterror(400);
					} else {
						#キャッシュを読むことにする
						$readcache = 1;	#@data = <RW>;
					}
				}
			}
		} else {
			$readcache = 1;
		}

		if($readcache == 1){
			if($rls == 0){
				for($i = 0;$i < $rto;$i++){
					$data[$i] = <RW>;
				}
			} else {
				for($i = 0;$i < $rls;$i++){
					$data[$i] = <RW>;
				}
			}
			$all = $data2[1];
		} else {
			$all = $#data+1;
		}
		
		close(RW);

		if($#data < 0){	#データが存在しない場合
			return &puterror(404);
		}

		if($rls == 0){	#ls=0
			$start = $rst - 1;	$to = $rto;
		} else {	#use ls
			$start = 0;	$to = $rls;
		}

		if($#data < $start){return &puterror(405);}	#読み込みスタートの位置よりもデータが少ないときはエラー

		$outmode = ' LIST';
	} else {		#レスの読み込み
		my ($time, $thinterval);
		$time = time();
		$interval = $time - (stat("$dir/$rbbs/$rth.dat"))[9];

		&openfile("$dir/$rbbs/$rth.dat");	#開けなくても新しくファイルを作らない
		$size = -s "$dir/$rbbs/$rth.dat";
		#$size2 = $size;

		$head = "";
		$head = <RW> if $size > 0;
		@data2 = split(/\t/ , $head);
		chomp($data2[1]);
		
		if($size > 0 && $data2[1] > 1) {
			$thinterval = ($time - $rth) / $data2[1] / $thintervalfactor;
			if($thinterval < $thmininterval) {
				$thinterval = $thmininterval;
			}
			if($thinterval > $thmaxinterval) {
				$thinterval = $thmaxinterval;
			}
		} else {
			$thinterval = $thmininterval;
		}

		if($head =~ /<>/){	#datファイルがそのまま放り込まれたとき
			$data[0] = $head;
			push(@data, <RW>);
			$all = $#data+1;
		} elsif($interval > $thinterval || $size == 0){	#レスを再読込する可能性のある場合
			if(!exists $ENV{MOD_PERL}){	#!mod_perl
				require 'http.pl';
			}
			$http'ua = $ua;
			$http'range = 0;
			$http'other = '';

			$headsize = length($head);
			$size -= $headsize;
			if($thuseims == 1){		#更新チェックを使用する
				if($data2[1] > 0){	#総レス数が存在しているとき
					if($data2[0] ne ''){	#Last-Modified欄が存在するとき
						$http'other = "If-Modified-Since: $data2[0]\r\n\r\n";
					}
					if($rls == 0 && $rto <= $data2[1]){	#読み込もうとしているデータはすでにキャッシュ内にあるとき
						&getResCache();

						if($data[$rto-1] eq ''){	# datに異常がある場合はデータを取得しなおす
							$http'other = '';		# If-Modified-Sinceの解除
							$readcache = -1;		# キャッシュを放棄する
						} else {
							$readcache = 1;			# キャッシュを使用する
						}
					}
				}
			}

			if($readcache != 1){
				$chkdat = '';
				if($readcache == 0){	# 通常の場合(キャッシュデータからの差分読み込みをする場合)
					# キャッシュの読み込み(データが更新されていなければここで読み込んだデータを転送する)
					&getResCache();
						$mop = "";
						if(($#data + 1) != $data2[1] || $#data < 0){	# datに異常がある場合
							$mhttp'other = '';							# If-Modified-Sinceの解除
						} elsif($readdiff == 1){						# 差分読み込みを行う場合
							$chkdat = $data[$#data];
							$size -= length($chkdat);
							if($rls > 0){
								if($rls <= 2){
									$rls = 3;
								}
								$mop = "l".($rls - 1);
							} else {
								if($rst > 1){
									if($rst == 1){
										$mop = "1-$rto";
									} else {
										$mop = ($rst-1) . "-$rto";
									}
								} else {
									$mop = "$rst-$rto";
								}
							}
							#if($size > 32){
							#	$http'range = $size;
							#	$_usegzip = $http'usegzip;				# usegzipの値を保存
							#	$http'usegzip = 0;						# rangeを使用する時はgzipは使えないのでusegzipを無効にする
							#} else {
							#	$chkdat = '';
							#}
						}

					if(($#data + 1) != $data2[1] || $#data < 0){	# datに異常がある場合はデータを取得しなおす
						$http'other = '';							# If-Modified-Sinceの解除
					} elsif($readdiff == 1){						# 差分読み込みを行う場合
						# あぼーんチェック用のデータを用意する(キャッシュ中の最新レス)
						$chkdat = $data[$#data];
						$size -= length($chkdat);
						if($size > 32){
							$http'range = $size;
							$_usegzip = $mhttp'usegzip;				# usegzipの値を保存
							$mhttp'usegzip = 0;						# rangeを使用する時はgzipは使えないのでusegzipを無効にする
						} else {
							# データサイズが小さすぎる場合は差分読み込みをキャンセルする
							$chkdat = '';
						}
					}
				}

				$reload = 0;
				$_usebg20 = $usebg20;
				if($rhost !~ /\.2ch\.net/) { $_usebg20 = 0; }
RELOAD:
				$str = &mhttp'get("$rhost/bbs/offlaw.cgi/$rbbs/$rth/$mop",0);		# ダウンロード
				warn "[DEBUG] $_usebg20 $size $readcache url:$rhost/$rbbs/dat/$rth.dat" if $debug;

				if($chkdat ne ''){$mhttp'usegzip = $_usegzip;}		# usegzipの値を元に戻す
				if($mhttp'header[0] =~ / 20[0-9] /){					# 正常に取得できた場合
					if($chkdat ne '' && substr($str, 0, length($chkdat)) ne $chkdat){	# DATが異常(あぼーんされている)
						$chkdat = '';
						$http'range = 0;
						$http'other = '';
						goto RELOAD;
					}

					if(!-e "$dir"){mkdir("$dir", $dirpermission);}	# ディレクトリがなければ作成する
					if(!-e "$dir/$rbbs"){mkdir("$dir/$rbbs", $dirpermission);}	# ディレクトリがなければ作成する

					if($str eq ''){									# ダウンロードエラー
						return &puterror(400);
					} else {
						if($chkdat ne ''){							# datを追記するとき
							@wdata = split(/(?<=\n)/, $str);
							splice(@data, $#data, 1, @wdata);
						} else {
							@data = split(/(?<=\n)/, $str);
						}

						$whead = getlastmodified(@mhttp'header) . "\t" . ($#data + 1) . "\n";	# Last-Modified、データの行数
						# 新形式のヘッダ
						$whead2 = sprintf("%s\t%04d\n", getlastmodified(@mhttp'header), ($#data + 1));	# Last-Modified、データの行数
						if(length($whead2) == $headsize){$whead = $whead2;}
						
						if($chkdat ne '' && length($whead) == $headsize){	# ヘッダのサイズが同じ場合追記する
							seek(RW, 0, 2);	#perl5.8.2だと追記に失敗するようなのでファイルポインタを最後に持って行く。
							for($i = 1;$i <= $#wdata;$i++){
								print RW $wdata[$i];
							}
							truncate(RW, tell(RW));

							seek(RW, 0, 0);	# ファイルポインタを先頭にセット
							print RW $whead;
						} else {											# ヘッダのサイズが異なる場合は全てを書く
							if(!-e "$dir/$rbbs/$rth.dat"){					# ファイルがなければ作成する
								&createfile("$dir/$rbbs/$rth.dat");
							} else {										# ファイルが存在する場合は部分的にしか読み込んでいないので追記時は一度全て読み直す
								if($chkdat ne ''){							# datを追記するとき
									seek(RW, 0, 0);							# ファイルポインタを先頭にセット
									$_ = <RW>;
									$i = 0;
									while(<RW>){
										$data[$i] = $_;
										$i++;
									}
								}
							}

							seek(RW, 0, 0);		# ファイルポインタを先頭にセット
							print RW $whead2;
							print RW @data;
							if(tell(RW) == 0){
								warn "write failed.. [$dir/$rbbs/$rth.dat][$tmppp]";
							} else {
								#warn "write ok.. [$dir/$rbbs/$rth.dat]";
								truncate(RW, tell(RW));
							}
						}
					}
				} elsif($mhttp'header[0] =~ / 302 /){				# dat落ち(たぶん)
					if($#data < 0 || $#data < ($rst - 1)){			# キャッシュが存在しないor存在しても読み込む位置の方が先の場合
						if($#data < 0 && -e "$dir/$rbbs/$rth.dat"){	# ファイルが存在する場合はそのファイルは壊れているので削除する
							unlink("$dir/$rbbs/$rth.dat");
						} else {
							foreach $str (@mhttp'header) {
								if($str =~ /^Location: .+\/_?403\//){	# 規制されていた時
									return &puterror(407);
								}
							}
						}
						return &puterror(403);
					} else {
						# キャッシュを読むことにする
					}
				} elsif($mhttp'header[0] =~ / 304 /){				# キャッシュを使用する(304 Not Modified)
					#キャッシュを読むことにする
				} elsif($mhttp'header[0] =~ / 416 /){				# DATに異常あり(あぼーんなど)(416 Requested Range Not Satisfiable)
					$chkdat = '';
					$http'range = 0;
					goto RELOAD;
				} else {											# 正常に取得できなかったときは保存しないで出力して終了
					if($#data < 0){	# キャッシュが存在しない場合
						return &puterror(400);
					} else {
						#キャッシュを読むことにする
					}
				}
				$all = $#data+1;
			}
		} else {	# キャッシュを使用する
			&getResCache();
		}
		close(RW);

		if($#data < 0){	#データが存在しない場合
			return &puterror(404);
		}

		#出力範囲調整
		if($rls == 0){	#ls=0
			$start = $rst - 1;	$to = $rto;
		} else {	#use ls
			$start = $#data - $rls + 1;	$to = $#data + 1;
			if($start < 0){$start = 0;}	#lsよりもデータが少ない場合
		}

		if($#data == ($start-1)){return &puterror(406);}	#読み込みスタートの位置よりもデータが1少ないときは新レス無しを出力
		elsif($#data < $start){	#読み込みスタートの位置よりもデータが少ないとき
			$buffer = "ERR - 405 ";	#エラーを出力する
			#return &puterror(405);

			#最新レスを出力する
			$rls = $rto - $rst + 1;

			$start = $#data - $rls + 1;	$to = $#data + 1;
			if($start < 0){$start = 0;}	#lsよりもデータが少ない場合
		}

		$title = '';
		if($rop !~ /f/i && $start > 0){
			$title = (split(/<>/, $data[0]))[4];
			if($#data >= $start && $start != 0){
				chomp($data[$start]);	$data[$start] .= $title;		#一番はじめのレスにタイトルを付ける
			}
		}

		$outmode = ' RES';
	}

	if($to > $#data){$to = $#data + 1;}			#読み込み終わり位置よりもデータが少ないときは最後まで読む

	#出力
	$buffer .= "Res:" . ($start+1) . "-$to/" . $all . "$outmode\n";	#ヘッダ(Res:x-y/all)
	if($rth != 0 && $start > 0 && $rop =~ /f/i){	#1番目のレスを出力
		$buffer .= $data[0];
	}
	for($i = $start;$i < $to;$i++){
		$buffer .= $data[$i];
	}

	return $buffer;
}

sub check {	# 更新チェック

	$rhost = $_[0], $rbbs = $_[1], $rth = $_[2], $rst = $_[3];
	
	&getdir();

	if($rth == 0){	#スレ一覧の読み込み
	} else {		#レスの読み込み
		$interval = time() - (stat("$dir/$rbbs/$rth.dat"))[9];

		&openfile("$dir/$rbbs/$rth.dat");	#開けなくても新しくファイルを作らない
		$head = <RW>;
		close(RW);

		# 全レス数の取得
		@data2 = split(/\t/ , $head);
		chomp($data2[1]);

		# キャッシュが指定した位置より進んでいる場合
		if($data2[1] >= $rst) {
			return 1;
		} else {
			# キャッシュチェック間隔より長い時間が経過していれば再読込を行い再度チェックする
			if($interval > $chkinterval){
				$buffer = &read($rhost, $rbbs, $rth, $rst, $rst, 0, "");
				if($buffer =~ /^Res:\d+\-\d+\/(\d+)/){
					if($1 >= $rst){
						return 1;
					}
				}
			}
		}
	}
	return 0;
}

sub getdir {
		$dir = "$dat/public_html/machibbs";

}

sub getResCache {		# レスのキャッシュを読み込む
	if($data2[1] <= 0 || $data2[1] < 100 || ($rls == 0 && $rst < $data2[1] / 2) || $rls > $data2[1] / 2 ){
		if($rls == 0 && $rto <= $data2[1]){
			for($i = 0;$i < $rto;$i++){
				$data[$i] = <RW>;
			}
			$all = $data2[1];
		} else {
			@data = <RW>;
			$all = $#data+1;
		}
	} else {
		if($rls == 0){
			if($rst <= $data2[1]){
				&readBackwards($rst, $data2[1]);
			} else {
				&readBackwards($data2[1], $data2[1]);
			}
		} else {
			&readBackwards($data2[1] - $rls, $data2[1]);
		}
		$all = $data2[1];
	}
}

# ファイルの最後からさかのぼってデータを読み込む
# 大きなファイルの後半部分を取得する場合はディスクアクセスを減らすことが出来る
sub readBackwards {
	my $from, $readto;
	my $buflen;
	my $nextend;
	my @buf, $buf;
	my $i;

	($from, $readto) = @_;

	$data[0] = <RW>;						# タイトルを読むために1行目を読み込む
	if($from < 0){$from = 1;}				# スタート位置の補正

	$buflen = 768 * ($readto - $from + 1);		# 1レスにつき768byte読み込む
	if($buflen < 4096){$buflen = 4096;}		# 2048バイト以下の場合は補正
	seek(RW, -$buflen, 2);					# SEEK_END

	while(1){
		$buflen = $buflen - length(<RW>);	# 行の途中かもしれないので1行目は捨てる。正確な読み込むバッファの長さを計算
		$nextend = tell(RW);				# 次のバッファ読み込み終了位置を取得

		# datが壊れている場合
		if($buflen <= 0){
			warn "datbroken detected! $#data $data2[1] $readto $#buf $from $i  $rhost $rbbs $rth $rst $rto $rls";
			@data = ();
			last;
		}

		read(RW, $buf, $buflen);
		@buf = split(/\n/, $buf);
		for($i = 0;$i <= $#buf; $i++){
			#if($readto - $#buf + $i - 1 < 0){
			#	warn "$readto $#buf $i  $rhost $rbbs $rth $rst $rto $rls";
			#}
			$data[$readto - $#buf + $i - 1] = $buf[$i] . "\x0A";
		}

		if($readto - $#buf <= $from){			# fromまで読みこんだら
			last;							# 終了
		} else {
			$buflen = 4096;					# 4K読む
			$readto = $readto - $#buf - 1;

			if($nextend - $buflen < 0){$buflen = $nextend;}
			seek(RW, $nextend - $buflen, 0);# SEEK_START
		}
	}
}

sub openfile {		# 読み書きモードで開く(開けなくても新しくファイルを作らない)
	if (open( RW, "+<$_[0]")) {
		if($win9x == 0){
			flock(RW, 2);			# ロック確認。ロック
		}
		binmode(RW);
	}
}

sub createfile {		# 読み書きモードで開く(開けなかったら新しくファイルを作る)
	if (open( RW, "+<$_[0]")) {
	} else {
		if(open( RW, ">$_[0]")){
		} else {return -1;}
	}
	if($win9x == 0){
		flock(RW, 2);				# ロック確認。ロック
	}
	binmode(RW);
}

sub getlastmodified {
	foreach $str (@_) {
		if($str =~ /^Last-Modified: (.+)$/i){
			return $1;
		}
	}
	return '';
}

sub decode {
	if ($ENV{'REQUEST_METHOD'} eq "POST") {
		binmode(STDIN);
		read(STDIN, $buffer, $ENV{'CONTENT_LENGTH'});
	} else {
		$buffer = $ENV{'QUERY_STRING'};
	}
	%FORM = ();

	foreach (split(/&/,$buffer)) {
		($name, $value) = split(/=/);
		$value =~ tr/+/ /;
		$value =~ s/%([a-fA-F0-9][a-fA-F0-9])/pack("C", hex($1))/eg;
		
		$FORM{$name} = $value;
	}
}

sub puterror {	#エラーの出力
	return "ERR - $_[0]\n";
}


return 1;

