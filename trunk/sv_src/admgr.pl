#!/usr/local/bin/perl

# ������ϥ�����ץ�

package pAdManager;

$adtype = "./ad/type.txt";
%adtype = ();

# ����ü���ξ��Τ߹������Ϥ���
sub putAdIfMobile {
	&putAd(shift) if &isMobile
}

# �������
sub putAd {
	$type = shift;
	
	# ���𥿥��פ��ɤ߹��ޤ�Ƥ��ʤ������ɤ߹���
	if (! $adtype{"__loaded__"}) {
		&loadAd;
	}
	
	# ���𥹥���ץȤμ¹�
	for (keys %adtype) {
		if ($_ eq $type) {
			do "./ad/$adtype{$type}";
		}
	}
}

# ���𥿥��פ��ɤ߹���
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
