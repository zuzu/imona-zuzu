#!/usr/local/bin/perl

####################################################################################################

#
# nts2.pl
#		// Next Thread Search for 2ch
#		// Mecab(Version 0.96)�����Ѥ��������ǲ��Ϥˤ�꼡����Ȼפ��륹��åɤ򸫤Ĥ���ѥå�����
#

####################################################################################################

package pNextThreadSearch;

BEGIN {	#���ư���Τ�
	## ���� ########################################################################################
	do 'setting.pl';
	################################################################################################
	
	##/libraries/########################
	require "jcode.pl";
	if($encodemod >= 1){	# Encode �⥸�塼�����Ѥ���
		require Encode;
		require Encode::JP::H2Z;
	}
	if($encodemod >= 2){	# Drk::Encode �⥸�塼�����Ѥ���
		require Drk::Encode;
		$DrkEncode = Drk::Encode->new( ascii => 0 );
	}
	require MeCab;
	####################################

}
use Data::Dumper;

# ����   : $threadname(sjis), $threadid, $subject.txt(sjis)
# ����� : ����(threadid<>threadname \t points)
sub getNearThreads {

	my ($thname, $subject);
	
	$thname = $_[0];
	$subject = $_[2];

	# ʸ�������ɤ��Ѵ�
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
	if($encodemod >= 2){											# Encode,Drk::Encode �⥸�塼�����Ѥ���
		Encode::from_to($_[0], 'shiftjis', 'euc-jp');				# euc���Ѵ�
		$_[0] = $DrkEncode->h2z($_[0], 'euc');						# Ⱦ��ʸ�������Ѥ��Ѵ�
		$_[0] = $DrkEncode->z2h($_[0], 'euc', {ASCII=>1, KANA=>0});	# ���ѵ����Ⱦ�Ѥ��Ѵ�
	} elsif($encodemod >= 1){										# Encode �⥸�塼�����Ѥ���
		Encode::from_to($_[0], 'shiftjis', 'euc-jp');				# euc���Ѵ�
		Encode::JP::H2Z::h2z(\$_[0]);								# ���Ѥ��Ѵ�
	} else {
		&jcode::convert(\$_[0], 'euc' , 'sjis', 'z');				# euc���Ѵ� + ���Ѥ��Ѵ�
	}
}

# ����åɥ����ȥ����Ϥ����ϥå��������
sub gethash {
	my $str_euc = $_[0];
	my $n = $m->parseToNode($str_euc);
	my %hash;
	my $hinsi;
	while ($n = $n->{next}) {
		$n->{feature} =~ /^(.+?),/;
		$hinsi = $1;
		
		# �ݥ���ȵ�§
		if ($hinsi =~ /̾��/) {					# ̾���6�ݥ���ȡ������������1�ݥ����
			if ($n->{feature} =~ /̾��,��/) {
				$hash{$n->{surface}} = 1;
			} else {
				$hash{$n->{surface}} = 6;
			}
		} elsif ($hinsi =~ /ư��/) {			# ư���2�ݥ����
			$hash{$n->{surface}} = 2;
		}
		
	#    printf("%s\t%s\t%d\n",
	#           $n->surface,          # ɽ��
	#           $n->feature,          # ���ߤ��ʻ�
	#           $n->cost              # ���η����ǤޤǤΥ�����
	#           );
	}
	return %hash;
}

1;