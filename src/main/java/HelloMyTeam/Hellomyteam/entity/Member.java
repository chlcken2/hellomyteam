package HelloMyTeam.Hellomyteam.entity;

import HelloMyTeam.Hellomyteam.entity.status.MemberStatus;
import lombok.Getter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "Member")
@Getter
public class Member {

    @Id
    @GeneratedValue
    @Column(name = "member_id")
    private Long id;
    private String mobile;                             //핸드폰번호
    private String email;                              //XXX@gmail.com
    private String memberName;                         //닉네임 -> 이름 사용 고정
    private String birthday;
    private MemberStatus memberStatus;                 //0-정상, 1-중지, 2-탈퇴, 3-경고, 4-강퇴, 5-불법
    private int termsOfServiceYn;                     //0-미수신, 1-수신
    private int privacyYn;                             //0-미수신, 1-수신

    @OneToMany(mappedBy = "member")
    private List<Board> board = new ArrayList<>();

    @OneToMany(mappedBy = "member")
    private List<OwnTeam> ownTeams = new ArrayList<>();

    @OneToMany(mappedBy = "member")
    private List<CommentReply> commentReplies = new ArrayList<>();

}
