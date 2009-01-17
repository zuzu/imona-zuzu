#!/usr/local/bin/perl

####################################################################################################

#
# nts2.pl
#		// Next Thread Search for 2ch
#		// Mecab(Version 0.96)を利用した形態素解析により次スレと思われるスレッドを見つけるパッケージ
#

####################################################################################################

package pNextThreadSearch;

BEGIN {	#初回起動時のみ
	## 設定 ########################################################################################
	do 'setting.pl';
	################################################################################################
	
	##/libraries/########################
	require "jcode.pl";
	if($encodemod >= 1){	# Encode モジュールを使用する
		require Encode;
		require Encode::JP::H2Z;
	}
	if($encodemod >= 2){	# Drk::Encode モジュールを使用する
		require Drk::Encode;
		$DrkEncode = Drk::Encode->new( ascii => 1 );
	}
	require MeCab;
	####################################

}
use Data::Dumper;

# 引数   : $threadname(sjis), $threadid, $subject.txt(sjis)
# 戻り値 : 配列(threadid<>threadname \t points)
sub getNearThreads {

	my ($thname, $subject);
	
	$thname = $_[0];
	$subject = $_[2];

	# 文字コードの変換
	&convencode($thname);
	&convencode($subject);

	my @data = split(/\n/, $subject);
	my @data2 = split(/\n/, $_[2]);
	local $m = new MeCab::Tagger("");
	my %hash = &gethash($thname);

	my %thread;
	my %tmphash;
	my ($val, $thinfo, $thinfosjis);
	%thread = ();
	%tmphash = ();
	for(0..$#data){
		$data[$_] =~ s/[\r\n]*$//g;
		$data2[$_] =~ s/[\r\n]*$//g;
		$thinfo = $data[$_];
		$thinfosjis = $data2[$_];
		if($thinfo =~ /(.+?)<>(.+?)\s+\(\d+\)$/){
			if($1 ne $_[1] && $1 ne "$_[1].dat"){
				%tmphash = &gethash($2);
				$val = 0;
				foreach (keys %tmphash) {
					$val += $hash{$_};
				}
				$thread{$thinfosjis} = $val;
			}
		}	
	}

	my @val;
	@val = ();
	foreach ( sort {$thread{$b} <=> $thread{$a}} keys %thread ) {
		push(@val, "$_\t" . $thread{$_});
	}
	return \@val;
}

sub convencode {
	if($encodemod >= 2){											# Encode,Drk::Encode モジュールを使用する
		Encode::from_to($_[0], 'shiftjis', 'euc-jp');				# eucに変換
		$_[0] = $DrkEncode->h2z($_[0], 'euc');						# 半角文字を全角に変換
		$_[0] = $DrkEncode->z2h($_[0], 'euc', {ASCII=>1, KANA=>0});	# 全角記号を半角に変換
	} elsif($encodemod >= 1){										# Encode モジュールを使用する
		Encode::from_to($_[0], 'shiftjis', 'euc-jp');				# eucに変換
		Encode::JP::H2Z::h2z(\$_[0]);								# 全角に変換
	} else {
		&jcode::convert(\$_[0], 'euc' , 'sjis', 'z');				# eucに変換 + 全角に変換
	}
}

# スレッドタイトルを解析したハッシュを得る
sub gethash {
	my $str_euc = $_[0];
	my $n = $m->parseToNode($str_euc);
	my %hash;
	my $hinsi;
	while ($n = $n->{next}) {
		$n->{feature} =~ /^(.+?),/;
		$hinsi = $1;
		
		# ポイント規則
		if ($hinsi =~ /名詞/) {					# 名詞は6ポイント。ただし数詞は1ポイント
			if ($n->{feature} =~ /名詞,数/) {
				$hash{$n->{surface}} = 1;
			} else {
				$hash{$n->{surface}} = 6;
			}
		} elsif ($hinsi =~ /動詞/) {							# 動詞は2ポイント
			$hash{$n->{surface}} = 2;
		}
		
	#    printf("%s\t%s\t%d\n",
	#           $n->surface,          # 表層
	#           $n->feature,          # 現在の品詞
	#           $n->cost              # その形態素までのコスト
	#           );
	}
	return %hash;
}

1;