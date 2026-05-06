package com.casino.blackjack.service;

import com.casino.blackjack.model.dto.CreditCardDTO;
import com.casino.blackjack.model.entity.CreditCardEntity;
import com.casino.blackjack.model.entity.RoleEntity;
import com.casino.blackjack.model.entity.UserEntity;
import com.casino.blackjack.model.enumerated.UserRoleEnum;
import com.casino.blackjack.model.view.CreditCardsManageView;
import com.casino.blackjack.repo.CreditCardRepository;
import com.casino.blackjack.service.auth.UserService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class CreditCardService {

    private final UserService userService;

    private final CreditCardRepository creditCardRepository;

    public CreditCardService(UserService userService, CreditCardRepository creditCardRepository) {
        this.userService = userService;
        this.creditCardRepository = creditCardRepository;
    }

    @Transactional
    public Optional<CreditCardEntity> registerNewCreditCard(CreditCardDTO creditCardDTO, Long currentUserId) {

        Optional<UserEntity> byId = userService.findById(currentUserId);

        if (byId.isEmpty()) {
            throw new IllegalStateException("You must be logged in to make card operations");
        }

        UserEntity currentUser = byId.get();

        CreditCardEntity creditCardEntity = new CreditCardEntity()
                .setCardNumber(creditCardDTO.getCardNumber())
                .setCardHolder(creditCardDTO.getCardHolder())
                .setExpiredMonth(creditCardDTO.getExpiredMonth())
                .setExpiredYear(creditCardDTO.getExpiredYear())
                .setCardCvc(creditCardDTO.getCardCvc())
                .setOwner(currentUser);

        return Optional.of(creditCardRepository.save(creditCardEntity));
    }

    public List<CreditCardDTO> getRegisteredCreditCards(Long currentUserId) {
        Optional<UserEntity> byId = userService.findById(currentUserId);

        if (byId.isEmpty()) {
            throw new IllegalStateException("You must be logged in to make card operations");
        }

        UserEntity currentUser = byId.get();

        List<CreditCardEntity> byOwner = creditCardRepository.findByOwner(currentUser);

        return byOwner.stream()
                .map(cce -> new CreditCardDTO()
                        .setCardCvc(cce.getCardCvc())
                        .setCardHolder(cce.getCardHolder())
                        .setExpiredYear(cce.getExpiredYear())
                        .setExpiredMonth(cce.getExpiredMonth())
                        .setCardNumber(cce.getCardNumber())
                ).toList();
    }

    public Optional<Long> getOwnerId(String cardNumber) {

        Optional<CreditCardEntity> byCardNumber = creditCardRepository.findByCardNumber(cardNumber);

        return byCardNumber.map(creditCardEntity -> creditCardEntity.getOwner().getId());
    }

    public Boolean checkCreditCardNumberAndCvcMatch(String cardNum, String cvc) {

        Long currentLoggedUserId = userService.getCurrentLoggedUserId();

        if (currentLoggedUserId == null) {
            return false;
        }

        Optional<CreditCardEntity> byCardNumber = creditCardRepository.findByCardNumber(cardNum);

        if (byCardNumber.isEmpty()) {
            return false;
        }

        CreditCardEntity creditCardEntity = byCardNumber.get();

        boolean cvcMatch = creditCardEntity.getCardCvc().toString().equals(cvc);
        boolean cardNumMatch = creditCardEntity.getCardNumber().equals(cardNum);
        boolean userIdMatch = creditCardEntity.getOwner().getId().equals(currentLoggedUserId);

        return cvcMatch && cardNumMatch && userIdMatch;
    }

    public Optional<CreditCardDTO> getByCardNumber(String cardNum) {
        Optional<CreditCardEntity> byCardNumber = creditCardRepository.findByCardNumber(cardNum);

        if (byCardNumber.isEmpty()) {
            return Optional.empty();
        }

        CreditCardEntity creditCardEntity = byCardNumber.get();

        CreditCardDTO creditCardDTO = new CreditCardDTO()
                .setCardCvc(creditCardEntity.getCardCvc())
                .setCardNumber(creditCardEntity.getCardNumber())
                .setExpiredYear(creditCardEntity.getExpiredYear())
                .setExpiredMonth(creditCardEntity.getExpiredMonth())
                .setCardHolder(creditCardEntity.getCardHolder());

        return Optional.of(creditCardDTO);
    }

    public List<CreditCardsManageView> getByOwnerId(Long ownerId) {
        List<CreditCardEntity> byOwnerId = creditCardRepository.findByOwnerId(ownerId);

        return byOwnerId.stream().map(c ->
                        new CreditCardsManageView().setId(c.getId())
                                .setCardNumber(c.getCardNumber())
                                .setCardHolder(c.getCardHolder())
                                .setExpiredYear(c.getExpiredYear())
                                .setExpiredMonth(c.getExpiredMonth()))
                .toList();
    }

    public Boolean isOwner(Long cardId, Long userId) {
        return isOwner(creditCardRepository.findById(cardId).orElse(null), userId);
    }

    private boolean isOwner(CreditCardEntity creditCardEntity, Long userId) {
        if (creditCardEntity == null || userId == null) {
            // anonymous users have not registered any credit cards
            // missing creditCard is meaningless
            return false;
        }

        Optional<UserEntity> byId = userService.findById(userId);

        if (byId.isEmpty()) {
            throw new IllegalStateException("anonymous user");
        }

        UserEntity userEntity = byId.get();

        if (isAdmin(userEntity)) {
            // all admins own all offers
            return true;
        }

        return Objects.equals(
                creditCardEntity.getOwner().getId(),
                userEntity.getId());
    }

    private Boolean isAdmin(UserEntity userEntity) {
        return userEntity.getRoles().stream()
                .map(RoleEntity::getRole)
                .anyMatch(r -> UserRoleEnum.ADMIN == r);
    }

    @Transactional
    public void deleteCard(Long cardId) {
        creditCardRepository.deleteById(cardId);
    }
}
