#!/usr/local/bin/perl

# �������Ĥμ�ư����
# ����ž��ư�б�
# ��brdflex.txt����
# ��Ԥ�������ץ�
#
# ʸ�������� EUC
# �ѹ��Բ�
#
# make brdflex.txt
#         soft.spdv.net

##/������ʬ/########################
$brd2 = 'brd2.txt';	# �ĥǡ���(board.cgi�Ǻ����������) ���ƥ�����Ĥ�̾��
$brd3 = 'brd3.txt';	# �ĥǡ���(board.cgi�Ǻ����������) ���ֹ�ݡ�URL�Ѵ���
$brd4 = 'brd4.txt';	# �������ĥǡ���(board.cgi�Ǻ����������) ���ƥ�����Ĥ�̾��
$brd5 = 'brd5.txt';	# �������ĥǡ���(board.cgi�Ǻ����������) ���ֹ�ݡ�URL�Ѵ���
$flexiblebrd = 'brdflex.txt';	# ver15�ʾ�ǻ��Ѥ����İ����ǡ��� ü���˽��Ϥ���

$ignore = " info.2ch.net epg.2ch.net movie.2ch.net watch.2ch.net ";		# ̵�뤹�륵����
$ignorecategory = " ����å� ���İ��� �ġ����� �ޤ��££� ¾�Υ����� ";	# ̵�뤹�륫�ƥ���
$lastcategory = "�ޤ��££�";											# �ǽ����ƥ���

$addboard = 1;	# �����Ĥ�ư���ɲä���
####################################

require 'editbrd.pl';
require 'http.pl';
require 'jcode.pl';


#�����
$http'ua = $ua;
$http'range = 0;
$http'other = '';

@category = ();
@brd = ();
$brdtmp = "";

$str = &http'get("http://menu.2ch.net/bbsmenu.html");		#���������

$str =~ s/\r\n/\n/;

&jcode'sjis2euc(*str, 'h'); # euc���Ѵ�+���ѥ��ʤ�Ⱦ�ѥ��ʤ��Ѵ�
&jcode'tr(*str, '��-����-�ڣ�-���ɡ������ǡá�����ʡˡСѡΡϡ����ܡ����䡩��������', '0-9A-Za-z"#$%&\'|\\^! (){}[]:;+*=<>?/_@-');	# �ʲ����Ĥ����
&jcode'euc2sjis(*str); # sjis���Ѵ�

&jcode'euc2sjis(*ignorecategory); # sjis���Ѵ�
&jcode'euc2sjis(*lastcategory); # sjis���Ѵ�

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
		if($ignore !~ / $server /){	#̵��ꥹ�Ȥ����äƤ��ʤ�
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

				# �Ĥ���ž���Ƥ����
				if($transerver ne $server) {
					print "Server transfered $cattmp $tmp from: $transerver to $server";

					# ��ž
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
	flock(FILE, 2);				# ��å���ǧ����å�
	seek(FILE, 0, 0);			# �ե�����ݥ��󥿤���Ƭ�˥��å�

	print FILE join("\n", @category);
	print FILE "\n\n";
	print FILE join("\n", @brd);

	truncate(FILE, tell(FILE));	# �ե����륵������񤭹�����������ˤ���
	close(FILE);
} else {
	open(FILE, "> $flexiblebrd");
	binmode(FILE);
	print FILE join("\n", @category);
	print FILE "\n\n";
	print FILE join("\n", @brd);
	close(FILE);
}

