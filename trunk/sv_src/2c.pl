#!/usr/local/bin/perl

# 2ch Cache system
#         soft.spdv.net

#������ϲ���
# 2ch��read.cgi������򤷤ơ�2ch����٤򲼤��륹����ץȤǤ���
# dat�ɤߤ�Ԥ��������ɤ߹����ʪ�ϥ���å��夵���Τǡ�
# ����å��奵���С��Ȥ��Ʊ��Ѥ��뤳�Ȥ��Ǥ��ޤ���
# �ͥåȥ���ۤ��Ǥ⡢������Ǥ���ѤǤ��ޤ���

#��������ˡ
# 1.CGI�ǸƤӽФ��Ȥ�
# �ƤӽФ���
# /2c.pl?host=BBB.2ch.net&bbs=AAA&th=123456789&st=1&to=100&op=f
# ls����Ѳ�ǽ�Ǥ���ξ��¸�ߤ�����ϡ�ls��ͥ�褵��ޤ���
# �ޤ���POST�����ǤΥꥯ�����Ȥ���ѤǤ��ޤ���
#
# 2.require '2c.pl';�ǻ��Ѥ���Ȥ�
# &p2chcache'read(HOST, BOARD, THREAD, START, END(TO), LAST, OPTION);
# ������
# &p2chcache'read('BBB.2ch.net', 'AAA', '123456789', '1', '100', '0', 'f');
# START��END�ϡ�LAST=0�λ��˻��Ѥ���ޤ���

# ���ץ����ˤĤ���
# f:first 1���ܤΥ쥹��Ʊ���˽��Ϥ��ޤ�

# ���줾�졢THREAD��̵�������İ����θƤӽФ���Ƚ�Ǥ��ޤ���
# ���Ϥ����ǡ�����rawmode������Ϥ���ǡ����򸺤餷���褦�ʴ����Ǥ�(Res:123-456/789����ʬ�������Ϥ��ޤ���)��
# �ޤ����쥹���ɤ߹��ߤξ�硢��Ƭ�Υ쥹�κǸ��ɬ������åɤΥ����ȥ뤬����褦�ˤʤäƤ��ޤ���
# ����ϡ�iMona�Ǥ�URL�Τߤǥ쥹���ɤ⤦�Ȥ����Ȥ��˻��Ѥ���ޤ���

#������
# �����ɤ߹��ॹ��åɤ�dat�ɤߤ�Ԥ��ޤ������ܡ���Ʃ�����ܡ�����˥������ѹ��Υ����å��������˽��褽���ˤʤ��Τǡ�
# ��ʬ�ɤ߹��ߤ�If-Modified-Since����Ѥ�����������Ƥ������Ϥ��٤ƤΥǡ�����Ƥӥ�������ɤ��ޤ���

# ���顼�Ϥʤ�٤��Ф��ʤ��褦�ˤ��Ƥ��ޤ��������ꤵ�줿�ϰϤΥ쥹��¸�ߤ��ʤ���硢
# �ޤ����̿��˼��Ԥ�������r.i�Τ褦��ERR - XXX ���顼ʸ����
# �Ȥ��ä��ǡ����������ܤ˽��Ϥ���ޤ��������ܰʹߤϲ��⤢��ޤ���
# XXX�˲�������Τ��Ϥޤ���ޤäƤ��ޤ��󡣤Ȥꤢ���������ߤϤ��٤�ERR - 400�Ƚ��Ϥ��Ƥ��ޤ���

# ��¸�����DAT�λ��ͤϡ��ե�����Σ����ܤ�Last-Modified\t�ǡ����ιԿ�(\t�ϥ���)
# ����¸����Ƥ��ꡢ�����ܤ���������*.dat,suject.txt�Υǡ�����Ʊ����Τ���¸����Ƥ��ޤ���
# ���ԥ����ɤ�\n(0x0A,\x0A)����Ѥ��ޤ���

# dat�κ�ʬ�ɤ߹��ߤϡ����߼������Ƥ���쥹�κǸ��ޤ��ϰϤ��ɤ߹��ߡ�
# ������Ǹ�Υ쥹���ɤ߹�����ǽ�Υ쥹�����פ��Ƥ������(���ܡ���ʤɤϤ����餯����Ƥ��ʤ�)�˹������ޤ���
# ���פ��ʤ��ä����ϡ�dat�ΤϤ��ᤫ���ɤ�ľ���ޤ���
# ������ˡ�ǤϤۤȤ�ɤʤ��Ȼפ��ޤ���dat��������ǽ���⤢��ޤ���
# �㤨����ť������������Υ�������Ʊ���������Υ쥹��Ʃ�����ܡ��󤵤줿���ʤɤǤ����ܺ٤ϳ䰦���ޤ���

#���������ɤ�Ȥ�
# ��\dat\����������̾\�פ�dat�ե�����򤽤Τޤ��������Ǥ���������
# ������̾����ˡ(���������Ĥ����ֹ�ȥ����ֹ�)�ǥ�����ɤळ�Ȥ��Ǥ��ޤ���

#����������
#07/06/11 ver5.1
#�������측�е�ǽ�����ٸ���
#06/10/24 ver5.0
#�������측�е�ǽ���ɲ�
#06/05/08 ver4.3
#������ѹ�®����åɹ��������å���ǽ���ɲ�(Alpha Version)
#06/01/23 ver4.2
#��subject.txt�κ��祭��å�����ѻ��֤��ꤹ�뤳�Ȥˤ�äư�ž�μ�ư�ɿ�˼��Ԥ��뤳�Ȥ����ä��Τ���������
#05/04/16 ver4.1
#�������뵬�����б�(���顼���ѹ�������å����ɤ߹��ߤζ���)
#05/02/08 ver4.0
#��dat���ɤߤ��б��ˤ��HDD���������ηڸ�
#��dat�˲����Τζ���
#04/02/17 ver3.0
#���İ�ž�μ�ư�������б�
#04/02/10 ver2.7
#���ե����륵�������礭������å�����ɤ߽񤭤���Ȥ��θ�Ψ�����夷��
#03/11/12 ver2.6
#��0�Х��ȤΥե����뤬��������ʤ��褦�ˤ���
#03/10/27 ver2.5
#���쥹�ֹ�λ��꤬���������Ȥ����顼�Ⱥǿ��쥹���֤��褦�ˤ���
#03/10/24 ver2.4
#��dat����Ǥ⥭��å��夷�Ƥ���ʬ���ɤ��褦�ˤ���
#03/10/12 ver2.3
#��dat����»�˶����ʤä�
#03/10/11 ver2.2
#��2ch������Ƥ���Ȥ��Ǥ⥨�顼����Ϥ����˥���å����ɽ������褦�ˤ���
#03/07/20 ver2.1
#��dat�κ�ʬ�ɤ߹��ߤ����Ԥ���Х�����
#������åɰ�������������ʤ��Х�����
#��2ch�ʳ����Ĥ��Ǽ����ǥ��쥯�ȥ���ѹ�(\dat\�ۥ���̾\��̾\)
#03/07/05 ver2.0
#��dat�κ�ʬ�ɤ߹��ߤΥ��ݡ��ȡ�dat�ե�����λ��;塢100%�μ¤Ǥʤ�dat��������ǽ���⤢��
#���嵭�˴�Ϣ���ƾ�����٤������ä�
#03/06/14 ver1.5
#�����̤�dat�ե����������Ȥ��ƻȤ���褦�ˤʤä�(readonly)
#���Х����
#03/03/02 ver1.0

package p2chcache;

#$ENV{'TZ'} = "JST-9";

BEGIN {	#���ư���Τ�
	## ���� ###########################################################################################
	do 'setting.pl';
	$ua = 'Monazilla/1.00 (iMona/1.0)';	#USER-AGENT
	###################################################################################################

	if(exists $ENV{MOD_PERL}){	#mod_perl��ư��Ƥ���Ȥ�
		require 'http.pl';
	}

	if($usenextthsearch == 1){
		require 'nts2.pl';
	}
}

if(caller() eq ''){	#require�ǸƤФ줿�ΤǤϤʤ����
	if($mode != 1){
		&decode;
		binmode(STDOUT);
		print "Content-type: text/plain\n\n";
		print &read($FORM{'host'}, $FORM{'bbs'}, $FORM{'th'}, $FORM{'st'}, $FORM{'to'}, $FORM{'ls'}, $FORM{'op'});
	}
	exit();
}

sub read {			# �������������åɤ��ɤ߹���
	$/ = "\x0A";	#���ԥ����ɤ�\x0A(LF)�ˤ��롣

	$rhost = $_[0], $rbbs = $_[1], $rth = $_[2], $rst = $_[3], $rto = $_[4], $rls = $_[5], $rop = $_[6];
	$readcache = 0, $head = '', $buffer = '', $outmode = '';
	@data = ();

	if($rst > $rto){	#start������to�����礭�����
		$rto = 1024;	#�Ǹ�ޤ��ɤ�
	}

	&getdir();

	if($rth == 0){	#����������ɤ߹���
		$interval = time() - (stat("$dir/$rbbs/subject.txt"))[9];

		&openfile("$dir/$rbbs/subject.txt");

		$head = <RW>;
		@data2 = split(/\t/ , $head);
		chomp($data2[1]);

		if($interval > $brmininterval){	#�����������ɹ�������
			if(!exists $ENV{MOD_PERL}){	#!mod_perl
				require 'http.pl';
			}
			$http'ua = $ua;
			$http'range = 0;
			$http'other = '';

			if($bruseims == 1){		#���������å�����Ѥ���
				if($data2[0] ne '' && $data2[1] > 1 && $brmaxinterval > $interval){	#Last-Modified��¸�ߤ��������ֳ֤�Ĺ�����ʤ����
					$http'other = "If-Modified-Since: $data2[0]\r\n\r\n";
				}
			}

			if($readcache != 1){
				$reload = 0;
RELOADBRD:
				$str = &http'get("$rhost/$rbbs/subject.txt");		#���������
				
				if($http'header[0] =~ / 304 /){	#����å������Ѥ���(304 Not Modified)
					$readcache = 1;
				} elsif($reload == 0 && ($http'header[0] =~ / 302 /)){	#�İ�ž(302 Found or 200 OK && $str eq "") #|| ($http'header[0] =~ / 20[0-9] / && length($str) <= 100)
					print "302 Found or 200 OK && str eq<hr>";
					foreach $str (@http'header) {
						if($str =~ /^Location: .+\/403\//){	# ��������Ƥ�����
							#����å�����ɤळ�Ȥˤ���
							$readcache = 1;
						}
					}
					

					if($readcache != 1){
						$http'range = 0;
						$http'other = '';
						$str = &http'get("$rhost/$rbbs/");		#���������


						if($str =~ m/window\.location\.href\=\"([^"]+)\"<\/script>/) {
							require 'editbrd.pl';

							&peditbrd'transfer($1);
							$1 =~ m|http://([^/]*)/|;
							$rhost = $1;
							$reload = 1;
							goto RELOADBRD;
						} else {
							if($#data < 0){	#����å��夬¸�ߤ��ʤ����
								return &puterror(400);
							} else {
								#����å�����ɤळ�Ȥˤ���
								$readcache = 1;
							}
						}
					}
				} elsif($http'header[0] =~ / 20[0-9] /){				#����˼����Ǥ������
					if(!-e "$dir"){mkdir("$dir", $dirpermission);}	#�ǥ��쥯�ȥ꤬�ʤ���к�������
					if(!-e "$dir/$rbbs"){mkdir("$dir/$rbbs", $dirpermission);}	#�ǥ��쥯�ȥ꤬�ʤ���к�������

					@data = split(/(?<=\n)/, $str);
					
					if($#data < 0){	#��������ɥ��顼
						return &puterror(400);
					} else {
						if(!-e "$dir/$rbbs/subject.txt"){&createfile("$dir/$rbbs/subject.txt");}			#�ե����뤬�ʤ���к�������
						seek(RW, 0, 0);			# �ե�����ݥ��󥿤���Ƭ�˥��å�
						print RW getlastmodified(@http'header) . "\t" . ($#data + 1) . "\n" . $str;	#Last-Modified���ǡ����ιԿ�
						truncate(RW, tell(RW));
					}
				} else {	#����˼����Ǥ��ʤ��ä��Ȥ�����¸���ʤ��ǽ�λ
					if($#data < 0){	#����å��夬¸�ߤ��ʤ����
						return &puterror(400);
					} else {
						#����å�����ɤळ�Ȥˤ���
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

		if($#data < 0){	#�ǡ�����¸�ߤ��ʤ����
			return &puterror(404);
		}

		if($rls == 0){	#ls=0
			$start = $rst - 1;	$to = $rto;
		} else {	#use ls
			$start = 0;	$to = $rls;
		}

		if($#data < $start){return &puterror(405);}	#�ɤ߹��ߥ������Ȥΰ��֤���ǡ��������ʤ��Ȥ��ϥ��顼

		$outmode = ' LIST';
	} else {		#�쥹���ɤ߹���
		$interval = time() - (stat("$dir/$rbbs/$rth.dat"))[9];

		&openfile("$dir/$rbbs/$rth.dat");	#�����ʤ��Ƥ⿷�����ե��������ʤ�
		$size = -s "$dir/$rbbs/$rth.dat";
		$size2 = $size;

		$head = <RW>;
		@data2 = split(/\t/ , $head);
		chomp($data2[1]);

		if($interval > $thmininterval || $size == 0){	#�쥹����ɹ������ǽ���Τ�����
			if($head =~ /<>/){	#dat�ե����뤬���Τޤ�������ޤ줿�Ȥ�
				$data[0] = $head;
				push(@data, <RW>);
				$all = $#data+1;
			} else {
				if(!exists $ENV{MOD_PERL}){	#!mod_perl
					require 'http.pl';
				}
				$http'ua = $ua;
				$http'range = 0;
				$http'other = '';

				$headsize = length($head);
				$size -= $headsize;
				if($thuseims == 1){		#���������å�����Ѥ���
					if($data2[1] > 0){	#��쥹����¸�ߤ��Ƥ���Ȥ�
						if($data2[0] ne ''){	#Last-Modified��¸�ߤ���Ȥ�
							$http'other = "If-Modified-Since: $data2[0]\r\n\r\n";
						}
						if($rls == 0 && $rto <= $data2[1]){	#�ɤ߹��⤦�Ȥ��Ƥ���ǡ����Ϥ��Ǥ˥���å�����ˤ���Ȥ�
							&getResCache();

							if($data[$rto-1] eq ''){	# dat�˰۾郎������
								$http'other = '';		# If-Modified-Since�β��
								$readcache = -1;		# ����å������������
							} else {
								$readcache = 1;			# ����å������Ѥ���
							}
						}
					}
				}

				if($readcache != 1){
					$chkdat = '';
					if($readcache == 0){	# �̾�ξ��
						# ����å�����ɤ߹���(�ǡ�������������Ƥ��ʤ���Ф������ɤ߹�����ǡ�����ž������)
						&getResCache();

						if(($#data + 1) != $data2[1] || $#data < 0){	# dat�˰۾郎������
							$http'other = '';							# If-Modified-Since�β��
						} elsif($readdiff == 1){						# ��ʬ�ɤ߹��ߤ�Ԥ����
							$chkdat = $data[$#data];
							$size -= length($chkdat);
							if($size > 32){
								$http'range = $size;
								$_usegzip = $http'usegzip;				# usegzip���ͤ���¸
								$http'usegzip = 0;						# range����Ѥ������gzip�ϻȤ��ʤ��Τ�usegzip��̵���ˤ���
							} else {
								$chkdat = '';
							}
						}
					}

					$reload = 0;
RELOAD:
					$str = &http'get("$rhost/$rbbs/dat/$rth.dat");		# ���������
					if($chkdat ne ''){$http'usegzip = $_usegzip;}		# usegzip���ͤ򸵤��᤹
					if($http'header[0] =~ / 20[0-9] /){					# ����˼����Ǥ������
						if($chkdat ne '' && substr($str, 0, length($chkdat)) ne $chkdat){	# DAT���۾�(���ܡ��󤵤�Ƥ���)
							$chkdat = '';
							$http'range = 0;
							$http'other = '';
							goto RELOAD;
						}

						if(!-e "$dir"){mkdir("$dir", $dirpermission);}	# �ǥ��쥯�ȥ꤬�ʤ���к�������
						if(!-e "$dir/$rbbs"){mkdir("$dir/$rbbs", $dirpermission);}	# �ǥ��쥯�ȥ꤬�ʤ���к�������

						if($str eq ''){									# ��������ɥ��顼
							return &puterror(400);
						} else {
							if($chkdat ne ''){							# dat���ɵ�����Ȥ�
								@wdata = split(/(?<=\n)/, $str);
								splice(@data, $#data, 1, @wdata);
							} else {
								@data = split(/(?<=\n)/, $str);
							}

							$whead = getlastmodified(@http'header) . "\t" . ($#data + 1) . "\n";	# Last-Modified���ǡ����ιԿ�
							# �������Υإå�
							$whead2 = sprintf("%s\t%04d\n", getlastmodified(@http'header), ($#data + 1));	# Last-Modified���ǡ����ιԿ�
							if(length($whead2) == $headsize){$whead = $whead2;}
							
							if($chkdat ne '' && length($whead) == $headsize){	# �إå��Υ�������Ʊ������ɵ�����
								seek(RW, 0, 2);	#perl5.8.2�����ɵ��˼��Ԥ���褦�ʤΤǥե�����ݥ��󥿤�Ǹ�˻��äƹԤ���
								for($i = 1;$i <= $#wdata;$i++){
									print RW $wdata[$i];
								}
								truncate(RW, tell(RW));

								seek(RW, 0, 0);	# �ե�����ݥ��󥿤���Ƭ�˥��å�
								print RW $whead;
							} else {											# �إå��Υ��������ۤʤ�������Ƥ��
								if(!-e "$dir/$rbbs/$rth.dat"){					# �ե����뤬�ʤ���к�������
									&createfile("$dir/$rbbs/$rth.dat");
								} else {										# �ե����뤬¸�ߤ��������ʬŪ�ˤ����ɤ߹���Ǥ��ʤ��Τ��ɵ����ϰ��������ɤ�ľ��
									if($chkdat ne ''){							# dat���ɵ�����Ȥ�
										seek(RW, 0, 0);							# �ե�����ݥ��󥿤���Ƭ�˥��å�
										$_ = <RW>;
										$i = 0;
										while(<RW>){
											$data[$i] = $_;
											$i++;
										}
									}
								}

								seek(RW, 0, 0);		# �ե�����ݥ��󥿤���Ƭ�˥��å�
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
					} elsif($http'header[0] =~ / 302 /){				# dat���(���֤�)
						if($#data < 0 || $#data < ($rst - 1)){			# ����å��夬¸�ߤ��ʤ�or¸�ߤ��Ƥ��ɤ߹�����֤�������ξ��
							if($#data < 0 && -e "$dir/$rbbs/$rth.dat"){	# �ե����뤬¸�ߤ�����Ϥ��Υե�����ϲ���Ƥ���ΤǺ������
								unlink("$dir/$rbbs/$rth.dat");
							} else {
								foreach $str (@http'header) {
									if($str =~ /^Location: .+\/403\//){	# ��������Ƥ�����
										return &puterror(407);
									}
								}
							}
							return &puterror(403);
						} else {
							# ����å�����ɤळ�Ȥˤ���
						}
					} elsif($http'header[0] =~ / 304 /){				# ����å������Ѥ���(304 Not Modified)
						#����å�����ɤळ�Ȥˤ���
					} elsif($http'header[0] =~ / 416 /){				# DAT�˰۾濫��(���ܡ���ʤ�)(416 Requested Range Not Satisfiable)
						$chkdat = '';
						$http'range = 0;
						goto RELOAD;
					} else {											# ����˼����Ǥ��ʤ��ä��Ȥ�����¸���ʤ��ǽ��Ϥ��ƽ�λ
						if($#data < 0){	# ����å��夬¸�ߤ��ʤ����
							return &puterror(400);
						} else {
							#����å�����ɤळ�Ȥˤ���
						}
					}
					$all = $#data+1;
				}
			}
		} else {	# ����å������Ѥ���
			&getResCache();
		}
		close(RW);

		if($#data < 0){	#�ǡ�����¸�ߤ��ʤ����
			return &puterror(404);
		}

		#�����ϰ�Ĵ��
		if($rls == 0){	#ls=0
			$start = $rst - 1;	$to = $rto;
		} else {	#use ls
			$start = $#data - $rls + 1;	$to = $#data + 1;
			if($start < 0){$start = 0;}	#ls����ǡ��������ʤ����
		}

		if($#data == ($start-1)){return &puterror(406);}	#�ɤ߹��ߥ������Ȥΰ��֤���ǡ�����1���ʤ��Ȥ��Ͽ��쥹̵�������
		elsif($#data < $start){	#�ɤ߹��ߥ������Ȥΰ��֤���ǡ��������ʤ��Ȥ�
			$buffer = "ERR - 405 ";	#���顼����Ϥ���
			#return &puterror(405);

			#�ǿ��쥹����Ϥ���
			$rls = $rto - $rst + 1;

			$start = $#data - $rls + 1;	$to = $#data + 1;
			if($start < 0){$start = 0;}	#ls����ǡ��������ʤ����
		}

		$title = '';
		if($rop !~ /f/i && $start > 0){
			$title = (split(/<>/, $data[0]))[4];
			if($#data >= $start && $start != 0){
				chomp($data[$start]);	$data[$start] .= $title;		#���֤Ϥ���Υ쥹�˥����ȥ���դ���
			}
		}

		$outmode = ' RES';
	}

	if($to > $#data){$to = $#data + 1;}			#�ɤ߹��߽������֤���ǡ��������ʤ��Ȥ��ϺǸ�ޤ��ɤ�

	#����
	$buffer .= "Res:" . ($start+1) . "-$to/" . $all . "$outmode\n";	#�إå�(Res:x-y/all)
	if($rth != 0 && $start > 0 && $rop =~ /f/i){	#1���ܤΥ쥹�����
		$buffer .= $data[0];
	}
	for($i = $start;$i < $to;$i++){
		$buffer .= $data[$i];
	}

	# �����측�е�ǽ(2ch�Τ�ͭ��)
	if($to == 1001 && $rth != 0 && $usenextthsearch == 1 && ($rhost =~ /[\.\/]2ch\.net/ || $rhost =~ /[\.\/]bbspink\.com/)){
		if($title eq ""){
			$title = (split(/<>/, $data[0]))[4];
		}
		
		&openfile("$dir/$rbbs/subject.txt");
		<RW>;	# �إå�
		while(<RW>){$subjectbuf .= $_;}
		close(RW);

		$buffer2 = "";
		$val = &pNextThreadSearch'getNearThreads($title, $rth, $subjectbuf);
		for(0..4){
			if(${$val}[$_] =~ /(\d+).dat<>(.+?)\s*\((\d+)\)\t(\d+)/){
				# �쥹����1001�Ǥʤ���ΤΤ������ݥ���Ȥ�20�ʾ�
				# �⤷���ϥ����ȥ��Ĺ����û����Τǥݥ���Ȥ�10�ʾ�Τ��
				if( $3 != 1001 && ($4 >= 20 || ($_ <= 2 && length($title) < 15 && $4 >= 10)) ){
					$buffer2 .= "$4pt:<a href=http://$rhost/$rbbs/$1/>$2</a><br>";
				}
			}
		}
		if($buffer2 ne ""){
			$buffer =~ s/(<>[^<>]*?\n)$//;
			$buffer .= "<br><br>iMonaNextThreadSEARCH<br>" . $buffer2 . $1;
		}
	}

	return $buffer;
}

sub check {	# ���������å�

	$rhost = $_[0], $rbbs = $_[1], $rth = $_[2], $rst = $_[3];
	
	&getdir();

	if($rth == 0){	#����������ɤ߹���
	} else {		#�쥹���ɤ߹���
		$interval = time() - (stat("$dir/$rbbs/$rth.dat"))[9];

		&openfile("$dir/$rbbs/$rth.dat");	#�����ʤ��Ƥ⿷�����ե��������ʤ�
		$head = <RW>;
		close(RW);

		# ���쥹���μ���
		@data2 = split(/\t/ , $head);
		chomp($data2[1]);

		# ����å��夬���ꤷ�����֤��ʤ�Ǥ�����
		if($data2[1] >= $rst) {
			return 1;
		} else {
			# ����å�������å��ֳ֤��Ĺ�����֤��вᤷ�Ƥ���к��ɹ���Ԥ����٥����å�����
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
	if($rhost =~ /[\.\/]2ch\.net/ || $rhost =~ /[\.\/]bbspink\.com/){	#2ch
		$dir = "$dat";
	} elsif($rhost eq "local"){	#local
		$dir = "$dat/public_dat";
	} else {
		$dir = "$dat/$rhost";
	}
}

sub getResCache {		# �쥹�Υ���å�����ɤ߹���
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
				&readBakcwards($rst, $data2[1]);
			} else {
				&readBakcwards($data2[1], $data2[1]);
			}
		} else {
			&readBakcwards($data2[1] - $rls, $data2[1]);
		}
		$all = $data2[1];
	}
}

sub readBakcwards {		# �Ǹ夫��ե�������ɤ߹���
	my $from, $readto;
	my $buflen;
	my $nextend;
	my @buf, $buf;
	my $i;

	($from, $readto) = @_;

	$data[0] = <RW>;						# �����ȥ���ɤि���1���ܤ��ɤ߹���
	if($from < 0){$from = 1;}				# �������Ȱ��֤�����

	$buflen = 768 * ($readto - $from + 1);		# 1�쥹�ˤĤ�768byte�ɤ߹���
	if($buflen < 4096){$buflen = 4096;}		# 2048�Х��Ȱʲ��ξ�������
	seek(RW, -$buflen, 2);					# SEEK_END

	while(1){
		$buflen = $buflen - length(<RW>);	# �Ԥ����椫�⤷��ʤ��Τ�1���ܤϼΤƤ롣���Τ��ɤ߹���Хåե���Ĺ����׻�
		$nextend = tell(RW);				# ���ΥХåե��ɤ߹��߽�λ���֤����

		# dat������Ƥ�����
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

		if($readto - $#buf <= $from){			# from�ޤ��ɤߤ������
			last;							# ��λ
		} else {
			$buflen = 4096;					# 4K�ɤ�
			$readto = $readto - $#buf - 1;

			if($nextend - $buflen < 0){$buflen = $nextend;}
			seek(RW, $nextend - $buflen, 0);# SEEK_START
		}
	}
}

sub openfile {		# �ɤ߽񤭥⡼�ɤǳ���(�����ʤ��Ƥ⿷�����ե��������ʤ�)
	if (open( RW, "+<$_[0]")) {
		if($win9x == 0){
			flock(RW, 2);			# ��å���ǧ����å�
		}
		binmode(RW);
	}
}

sub createfile {		# �ɤ߽񤭥⡼�ɤǳ���(�����ʤ��ä��鿷�����ե��������)
	if (open( RW, "+<$_[0]")) {
	} else {
		if(open( RW, ">$_[0]")){
		} else {return -1;}
	}
	if($win9x == 0){
		flock(RW, 2);				# ��å���ǧ����å�
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

sub puterror {	#���顼�ν���
	return "ERR - $_[0]\n";
}


if($mode == 2){
	return 0;		#require�Ǥ��ɤ߹��ߤ���ݤ���
} else {
	return 1;
}
