#!/usr/local/bin/perl

# 広告出力スクリプト

package pAdManager;

$adtype = "./ad/type.txt";
%adtype = ();

# 携帯端末の場合のみ広告を出力する
sub putAdIfMobile {
	&putAd(shift) if &isMobile
}

# 広告出力
sub putAd {
	$type = shift;
	
	# 広告タイプが読み込まれていない場合は読み込む
	if (! $adtype{"__loaded__"}) {
		&loadAd;
	}
	
	# 広告スクリプトの実行
	for (keys %adtype) {
		if ($_ eq $type) {
			do "./ad/$adtype{$type}";
		}
	}
}

# 広告タイプの読み込み
sub loadAd {
	$adtype{"__loaded__"} = 1;

	if(open(IN, "$adtype")){
		binmode(IN);
		
		while (<IN>) {
			if (/^\#/) { 
			} else {
				my @type;
				s/[\r\n]*$//;
				@type = split(/[\t\s]+/);
				$adtype{$type[0]} = $type[1];
			}
		}
		close(IN);
	}
}

sub isMobile {
	if($ENV{'HTTP_USER_AGENT'} =~ m/SoftBank/i){	# SoftBank
		return 1;
	} elsif($ENV{'HTTP_USER_AGENT'} =~ m/Vodafone/i){	#Vodafone
		return 1;
	} elsif($ENV{'HTTP_USER_AGENT'} =~ m/J-PH/i){	#J-PHONE
		return 1;
	} elsif($ENV{'HTTP_USER_AGENT'} =~ m/DoCoMo/i){		# DoCoMo
		return 1;
	} elsif($ENV{'HTTP_USER_AGENT'} =~ m/KDDI/i){		# au
		return 1;
	} else {
		return 0;
	}
}

1;
