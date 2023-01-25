package HelloMyTeam.Hellomyteam.api;

import HelloMyTeam.Hellomyteam.dto.TeamJoinParam;
import HelloMyTeam.Hellomyteam.dto.TeamParam;
import HelloMyTeam.Hellomyteam.dto.TeamSearchCond;
import HelloMyTeam.Hellomyteam.dto.TeamSearchParam;
import HelloMyTeam.Hellomyteam.entity.Member;
import HelloMyTeam.Hellomyteam.entity.Team;
import HelloMyTeam.Hellomyteam.payload.CommonResponse;
import HelloMyTeam.Hellomyteam.service.MemberService;
import HelloMyTeam.Hellomyteam.service.TeamService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/team")
public class TeamController {

    private final TeamService teamService;
    private final MemberService memberService;

    @ApiOperation(value = "팀 생성", notes = "팀 생성: 회원테이블에 member_id가 존재해야한다.")
    @PostMapping("/create")
    public CommonResponse<?> saveTeam(@RequestBody TeamParam.TeamInfo teamInfo) {
        Team team = teamService.createTeamWithAuthNo(teamInfo);
        Member member = memberService.findMemberByTeamInfo(teamInfo);
        teamService.teamMemberInfoSaveAuthLeader(team, member);
        return CommonResponse.createSuccess(team);
    }

    @ApiOperation(value = "팀 찾기", notes = "팀 이름 혹은 팀 고유번호 입력, ")
    @PostMapping("/find")
    public CommonResponse<?> findTeam (@RequestBody TeamSearchCond condition) {
        List<TeamSearchParam> findTeams = teamService.findTeamBySearchCond(condition);
        return CommonResponse.createSuccess(findTeams);
    }


    @ApiOperation(value = "팀 가입신청", notes = "team_id를 통한 팀 가입신청, 가입 수락 전 권한 = WAIT")
    @ApiResponse(
        code = 200
        , message = "첫 신청시 true, 중복 신청시 false를 반환합니다."
    )
    @PostMapping("/join")
    public CommonResponse<?> joinTeam(@RequestBody TeamJoinParam teamJoinParam) {
        Member member = memberService.findMemberById(teamJoinParam);
        Team team = teamService.findTeamById(teamJoinParam);
        Boolean result = teamService.joinTeamAuthWait(team, member);
        return CommonResponse.createSuccess(result, "중복가입 체크적용, 가입신청 성공 = true, 가입 신청 실패 = false");
    }
    //팀 수락
}
