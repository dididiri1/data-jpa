package study.datajpa.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import study.datajpa.dto.MemberDto;
import study.datajpa.entity.Member;
import study.datajpa.repository.MemberRepository;

import javax.annotation.PostConstruct;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberRepository memberRepository;

    @GetMapping("/member/{id}")
    public String findMember(@PathVariable("id") Long id) {
        Member member = memberRepository.findById(id).get();

        return member.getUsername();

    }

    @GetMapping("/member2/{id}")
    public String findMember2(@PathVariable("id") Member member) {
        return member.getUsername();

    }
    
    @GetMapping("/members")
    public Page<MemberDto> list(@PageableDefault(size = 10, sort = "username") Pageable pageable) {
        return memberRepository.findAll(pageable).map(MemberDto::new);
    }

    /**
     * 1개 데이터 생성
     */
//    @PostConstruct
//    public void init () {
//        memberRepository.save(new Member("userA", 10));
//    }

    /**
     * 100개 데이터 생성
     */
//    @PostConstruct
//    public void init () {
//        for (int i = 0; i < 100; i++) {
//            memberRepository.save(new Member("user"+ i, i));
//        }
//
//    }
}
