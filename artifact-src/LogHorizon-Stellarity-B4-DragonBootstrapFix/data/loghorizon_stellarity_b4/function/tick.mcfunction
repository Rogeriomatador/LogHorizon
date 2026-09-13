# Repara SOMENTE o bootstrap da primeira luta do dragao do Stellarity.
# Espera um jogador entrar na ilha central e o dragao vanilla ja ter sido removido.
execute in minecraft:the_end as @e[type=minecraft:marker,tag=stellarity.exit_portal,tag=stellarity.post_gen.initialized,tag=!loghorizon.b4.dragon_bootstrap_done,tag=!stellarity.in_dragon_fight,tag=!stellarity.portal_activated] at @s if entity @a[distance=..256] unless entity @e[type=minecraft:ender_dragon] unless score @s stellarity.dragon.times_killed matches 1.. run function loghorizon_stellarity_b4:bootstrap
